package org.wingsofcarolina.quiz.extensions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.wingsofcarolina.quiz.common.FlashMessage;

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
		sb.append("<link rel=\"stylesheet\" href=\"/static/common.css\">");
		sb.append("<div class=\"topnav\">\n" + 
				"<a class=\"active\" href=\"/\">Home</a>\n");
		if (items != null) {
			for (String item : items) {
				List<String> components = Arrays.asList(item.split("\\s*:\\s*"));
				sb.append("<a href=\"");
				sb.append(components.get(0));
				sb.append("\">");
				sb.append(components.get(1));
				sb.append("</a>\n");
			}
		}
		sb.append("<a href=\"/profile\">Profile</a>\n");
		sb.append("<div style=\"float:right\"><a href=\"/api/logout\">Logout</a></div>\n</div>");
		
		String flash = FlashMessage.message();
		if (flash != null) {
			sb.append("<div id=\"flash\">" + flash + "</div>\n");
			sb.append("<script>setInterval(function(){$(\"#flash\").hide('slow');}, 10000);</script>");
		}
		return sb.toString();
	}
}