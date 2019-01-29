package org.wingsofcarolina.quiz.extensions;

import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class Color extends InlineMacroProcessor {
	
	public Color(String macroName) {
		super(macroName);
	}

	@Override
	public String process(ContentNode parent, String target, Map<String, Object> attributes) {
		StringBuffer sb = new StringBuffer();
		sb.append("<span style=\"color:" + target + "\">");
		sb.append(attributes.get("1"));
		sb.append("</span>");
		return sb.toString();
	}
}