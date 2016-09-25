/*
 * pragmatickm-password-taglib - Passwords nested within SemanticCMS pages and elements in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of pragmatickm-password-taglib.
 *
 * pragmatickm-password-taglib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * pragmatickm-password-taglib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with pragmatickm-password-taglib.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pragmatickm.password.taglib;

import com.aoindustries.encoding.Coercion;
import com.aoindustries.io.TempFileList;
import com.aoindustries.io.buffer.AutoTempFileWriter;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.io.buffer.BufferWriter;
import com.aoindustries.io.buffer.SegmentedWriter;
import com.aoindustries.servlet.filter.TempFileContext;
import static com.aoindustries.taglib.AttributeUtils.resolveValue;
import com.aoindustries.taglib.StyleAttribute;
import com.pragmatickm.password.model.Password;
import com.pragmatickm.password.model.PasswordTable;
import com.pragmatickm.password.servlet.impl.PasswordTableImpl;
import com.semanticcms.core.model.ElementContext;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.taglib.ElementTag;
import java.io.IOException;
import java.io.Writer;
import javax.el.ELContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

public class PasswordTableTag extends ElementTag<PasswordTable> implements StyleAttribute {

	private Object header;
	public void setHeader(Object header) {
		this.header = header;
	}

	private Object passwords;
	public void setPasswords(Object passwords) {
		this.passwords = passwords;
	}

	private Object style;
	@Override
	public void setStyle(Object style) {
		this.style = style;
	}

	@Override
	protected PasswordTable createElement() {
		return new PasswordTable();
	}

	@Override
	protected void evaluateAttributes(PasswordTable element, ELContext elContext) throws JspTagException, IOException {
		super.evaluateAttributes(element, elContext);
		element.setHeader(resolveValue(header, String.class, elContext));
	}

	private BufferResult writeMe;
	@Override
	protected void doBody(PasswordTable passwordTable, CaptureLevel captureLevel) throws JspException, IOException {
		try {
			super.doBody(passwordTable, captureLevel);
			if(captureLevel == CaptureLevel.BODY) {
				final PageContext pageContext = (PageContext)getJspContext();
				final ELContext elContext = pageContext.getELContext();
				final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

				// Evaluate expressins
				@SuppressWarnings("unchecked")
				Iterable<? extends Password> passwordIter = resolveValue(passwords, Iterable.class, elContext);
				Object styleObj = Coercion.nullIfEmpty(resolveValue(style, Object.class, elContext));

				// Enable temp files if temp file context active
				BufferWriter capturedOut = TempFileContext.wrapTempFileList(
					new SegmentedWriter(),
					request,
					// Java 1.8: AutoTempFileWriter::new
					new TempFileContext.Wrapper<BufferWriter>() {
						@Override
						public BufferWriter call(BufferWriter original, TempFileList tempFileList) {
							return new AutoTempFileWriter(original, tempFileList);
						}
					}
				);
				try {
					PasswordTableImpl.writePasswordTable(
						pageContext.getServletContext(),
						request,
						(HttpServletResponse)pageContext.getResponse(),
						capturedOut,
						passwordTable,
						passwordIter,
						styleObj
					);
				} finally {
					capturedOut.close();
				}
				writeMe = capturedOut.getResult();
			} else {
				writeMe = null;
			}
		} catch(ServletException e) {
			throw new JspTagException(e);
		}
	}

	@Override
	public void writeTo(Writer out, ElementContext context) throws IOException {
		if(writeMe != null) writeMe.writeTo(out);
	}
}
