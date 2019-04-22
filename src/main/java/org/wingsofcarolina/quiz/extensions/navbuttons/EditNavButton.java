package org.wingsofcarolina.quiz.extensions.navbuttons;

public class EditNavButton extends NavButton {

	@Override
	public String html() {
		StringBuffer sb = new StringBuffer();
		sb.append("<script>function modifyQuestion() { console.log(\"modify question\"); }</script>");
		sb.append("<a onclick=modifyQuestion()>Modify Question</a>\n");
		return sb.toString();
	}
}
