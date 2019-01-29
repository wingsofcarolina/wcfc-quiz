package org.wingsofcarolina.quiz.extensions;

import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class Flash extends InlineMacroProcessor {
	
	public Flash(String macroName) {
		super(macroName);
	}

	@Override
	public String process(ContentNode parent, String target, Map<String, Object> attributes) {
		String flash = org.wingsofcarolina.quiz.common.FlashMessage.message();
		if (flash != null) {
			return("<div style=\"background-color:#87162d; color:white; padding:10px; overflow:hidden;\" >" + flash + "</div>");
		} else {
			return null;
		}
	}
}