package org.wingsofcarolina.quiz.extensions.navbuttons;

public class ProfileNavButton extends NavButton {

	@Override
	public String html() {
		StringBuffer sb = new StringBuffer();
		sb.append("<a ");
		if (active) sb.append("class=\"active\"");
		sb.append(" href=\"/profile\">Profile</a>\n");
		return sb.toString();
	}
}
