package org.wingsofcarolina.quiz.extensions.navbuttons;

public class EditNavButton extends NavButton {

	@Override
	public String html() {
		StringBuffer sb = new StringBuffer();
		sb.append("<script>function modifyQuestion() {"
				+ "var questionId = prompt(\"Question ID\");"
				+ "if (questionId != null) { window.location.href = \"/editQuestion/\" + questionId + \"\";\n" + 
				" } }</script>");
		sb.append("<a onclick=modifyQuestion()>Modify Question</a>\n");
		return sb.toString();
	}
}
