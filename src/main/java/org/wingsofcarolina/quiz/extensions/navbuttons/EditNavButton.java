package org.wingsofcarolina.quiz.extensions.navbuttons;

public class EditNavButton extends NavButton {

	@Override
	public String html() {
		StringBuffer sb = new StringBuffer();
		sb.append("<script>function modifyQuestion() {"
				+ "var questionId = prompt(\"Question ID\");"
				+ "if (questionId != null) { window.location.href = \"/editQuestion/\" + questionId + \"\";\n" + 
				" } }</script>");
		sb.append("<a ");
		if (active) sb.append("class=\"active\"");
		sb.append(" onclick=modifyQuestion()>Edit Question</a>\n");
		return sb.toString();
	}
}
