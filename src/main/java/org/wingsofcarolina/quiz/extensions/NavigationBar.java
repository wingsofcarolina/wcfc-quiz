package org.wingsofcarolina.quiz.extensions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.wingsofcarolina.quiz.common.Flash;
import org.wingsofcarolina.quiz.common.Flash.Message;
import org.wingsofcarolina.quiz.extensions.navbuttons.NavButton;

public class NavigationBar extends InlineMacroProcessor {

	public NavigationBar(String macroName) {
		super(macroName);
	}

	@Override
	public String process(ContentNode parent, String target, Map<String, Object> attributes) {
		List<String> items = null;
		
		if (attributes.size() > 0) {
			String buttons = (String) attributes.get("buttons");
			items = Arrays.asList(buttons.split("\\s*;\\s*"));
		}

		StringBuffer sb = new StringBuffer();
		sb.append("<link rel=\"stylesheet\" href=\"/static/quiz-style.css\">");
		sb.append("<div class=\"topnav\">\n" + 
				"<a class=\"active\" href=\"/\">Home</a>\n");
		if (items != null) {
			for (String item : items) {
				Class<?> clazz;
				try {
					clazz = Class.forName("org.wingsofcarolina.quiz.extensions.navbuttons." + item);
					NavButton button = (NavButton) clazz.newInstance();
					sb.append(button.html());
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}
		sb.append("<a href=\"/profile\">Profile</a>\n");
		sb.append("<div style=\"float:right\"><a href=\"/api/logout\">Logout</a></div>\n</div>");
		
		Message flash = Flash.message();
		if (flash != null) {
			sb.append(flash.getDiv());
			sb.append("<script>setInterval(function(){$(\"#flash\").hide('slow');}, 10000);</script>");
		}
		return sb.toString();
	}
}