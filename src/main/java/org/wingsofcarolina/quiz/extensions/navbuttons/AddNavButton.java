package org.wingsofcarolina.quiz.extensions.navbuttons;

public class AddNavButton extends NavButton {

	@Override
	public String html() {
		StringBuffer sb = new StringBuffer();
		sb.append("<a href=\"/addQuestion\">Add Question</a>\n");
		return sb.toString();
	}
}
