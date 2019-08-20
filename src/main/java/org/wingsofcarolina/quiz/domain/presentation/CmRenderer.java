package org.wingsofcarolina.quiz.domain.presentation;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CmRenderer {
	private static final Logger LOG = LoggerFactory.getLogger(CmRenderer.class);

	private static Parser parser = Parser.builder().build();
	private static HtmlRenderer renderer = HtmlRenderer.builder().build();

	public static String render(String input) {
		Node document = parser.parse(input);
		String output = renderer.render(document);
		String result = remove(output, "<p>");
		result = remove(result, "</p>");
		return result;
	}
	
	public static String remove(String input, String cut) {
		StringBuilder sb = new StringBuilder(input);
		if (input.length() < cut.length()) {
			return input;
		} else if (input.indexOf(cut) != -1) {
			int start = input.indexOf(cut);
			StringBuilder afterRemove = sb.delete(start, start + cut.length());
			return afterRemove.toString();
		} else {
			return input;
		}
	}
}
