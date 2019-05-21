package org.wingsofcarolina.quiz.domain.presentation;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

public class CmRenderer {
	private static Parser parser = Parser.builder().build();
	private static HtmlRenderer renderer = HtmlRenderer.builder().build();

	public static String render(String input) {
		Node document = parser.parse(input);
		String output = renderer.render(document);
		return output.substring(3,output.length()-5);
	}
}
