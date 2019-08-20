package org.wingsofcarolina.quiz.domain.presentation;

import org.commonmark.node.Node;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import java.util.Arrays;
import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;

public class CmRenderer {
	private static List<Extension> extensions = Arrays.asList(TablesExtension.create());
	private static Parser parser = Parser.builder()
	        .extensions(extensions)
	        .build();
	private static HtmlRenderer renderer = HtmlRenderer.builder()
	        .extensions(extensions)
	        .build();
	
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
