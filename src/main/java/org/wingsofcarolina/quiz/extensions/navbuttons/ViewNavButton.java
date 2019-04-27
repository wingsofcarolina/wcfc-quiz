package org.wingsofcarolina.quiz.extensions.navbuttons;

public class ViewNavButton extends NavButton {

	@Override
	public String html() {
		StringBuffer sb = new StringBuffer();
		sb.append("<script>function viewQuestion() {"
				+ "var questionId = prompt(\"Question ID\");"
				+ "if (questionId != null) { window.location.href = \"/question/\" + questionId + \"\";\n" + 
				" } }</script>");
		sb.append("<a onclick=viewQuestion()>View Question</a>\n");
		return sb.toString();
	}
}
