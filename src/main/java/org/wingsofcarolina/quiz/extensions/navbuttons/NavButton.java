package org.wingsofcarolina.quiz.extensions.navbuttons;

public abstract class NavButton {

  protected boolean active = false;

  public abstract String html();

  public void setActive() {
    active = true;
  }
}
