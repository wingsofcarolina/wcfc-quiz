package org.wingsofcarolina.quiz.extensions.navbuttons;

public class AddNavButton extends NavButton {

  @Override
  public String html() {
    StringBuffer sb = new StringBuffer();
    sb.append("<a ");
    if (active) sb.append("class=\"active\"");
    sb.append(" href=\"/addQuestion\">Add Question</a>\n");
    return sb.toString();
  }
}
