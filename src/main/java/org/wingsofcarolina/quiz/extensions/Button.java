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
		StringBuffer sb = new StringBuffer();
		sb.append("<form action=\"" + target + "\" method=\"post\">");
		sb.append("<input type=\"submit\" value=\"" + attributes.get("label") + "\" />");
		sb.append("</form>");
		return sb.toString();
	}
}