package org.wingsofcarolina.quiz.extensions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.asciidoctor.ast.ContentNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.wingsofcarolina.quiz.common.FlashMessage;

public class NavigationBar extends InlineMacroProcessor {
	
	// TODO: Move this to the .css file, eventually
//	private String style = "<style>\n" + 
//			"/* Create background for flash messages */\n" + 
//			".flash {\n" + 
//			"  border-radius: 25px;\n" + 
//			"  background-color: #87162d;\n" + 
//			"  color: white;\n" + 
//			"  padding: 10px;\n" + 
//			"  overflow: hidden;\n" + 
//			"}\n" + 
//			"\n" + 
//			"/* Add a black background color to the top navigation */\n" + 
//			".topnav {\n" + 
//			"  border-radius: 25px;\n" + 
//			"  background-color: #555;\n" + 
//			"  overflow: hidden;\n" + 
//			"}\n" + 
//			"\n" + 
//			"/* Style the links inside the navigation bar */\n" + 
//			".topnav a {\n" + 
//			"  float: left;\n" + 
//			"  color: #f2f2f2;\n" + 
//			"  text-align: center;\n" + 
//			"  padding: 14px 16px;\n" + 
//			"  text-decoration: none;\n" + 
//			"  font-size: 17px;\n" + 
//			"}\n" + 
//			"\n" + 
//			"/* Change the color of links on hover */\n" + 
//			".topnav a:hover {\n" + 
//			"  background-color: #ddd;\n" + 
//			"  color: black;\n" + 
//			"}\n" + 
//			"\n" + 
//			"/* Add a color to the active/current link */\n" + 
//			".topnav a.active {\n" + 
//			"  background-color: #4CAF50;\n" + 
//			"  color: white;\n" + 
//			"}\n" + 
//			"</style>\n";

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
//		sb.append(style);
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
		sb.append("<a href=\"/student/profile/" + target + "\">Profile</a>\n");
		sb.append("<div style=\"float:right\"><a href=\"/api/logout\">Logout</a></div>\n</div>");
		
		String flash = FlashMessage.message();
		if (flash != null) {
			sb.append("<div id=\"flash\" class=\"flash\">" + flash + "</div>\n");
			sb.append("<script>setInterval(function(){document.getElementById(\"flash\").style.display = \"none\";}, 10000);</script>");
//			sb.append("<script>\n" + 
//					"document.getElementById(\"flash\").innerHTML = \"Hello JavaScript!\";\n" + 
//					"</script>");
					
		}
		return sb.toString();
	}
}