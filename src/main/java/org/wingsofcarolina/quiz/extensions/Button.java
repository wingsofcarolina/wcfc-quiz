package org.wingsofcarolina.quiz.extensions;

import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class Button extends InlineMacroProcessor {
	
	public Button(String macroName) {
		super(macroName);
	}

	@Override
	public String process(ContentNode parent, String target, Map<String, Object> attributes) {
		String method = (String) attributes.get("method");
		if (method == null) {
			method = "post";
		}
		StringBuffer sb = new StringBuffer();
		sb.append("<form action=\"" + target + "\" method=\"" + method + "\">");
		sb.append("<input type=\"submit\" value=\"" + attributes.get("label") + "\" />");
		sb.append("</form>");
		return sb.toString();
	}
}