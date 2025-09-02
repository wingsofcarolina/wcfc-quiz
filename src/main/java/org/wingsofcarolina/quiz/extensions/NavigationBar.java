package org.wingsofcarolina.quiz.extensions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.wingsofcarolina.quiz.common.Flash;
import org.wingsofcarolina.quiz.common.Flash.Message;
import org.wingsofcarolina.quiz.extensions.navbuttons.NavButton;

public class NavigationBar extends InlineMacroProcessor {

  public NavigationBar(String macroName) {
    super(macroName);
  }

  @Override
  public PhraseNode process(
    StructuralNode parent,
    String target,
    Map<String, Object> attributes
  ) {
    List<String> items = null;

    if (attributes.size() > 0) {
      String buttons = (String) attributes.get("buttons");
      items = Arrays.asList(buttons.split("\\s*;\\s*"));
    }
    String active = (String) attributes.get("active");
    if (active == null) {
      active = "";
    }

    StringBuffer sb = new StringBuffer();
    sb.append("<div class=\"topnav\">\n");
    if (items != null) {
      for (String item : items) {
        Class<?> clazz;
        try {
          clazz = Class.forName("org.wingsofcarolina.quiz.extensions.navbuttons." + item);
          NavButton button = (NavButton) clazz.newInstance();
          if (item.contentEquals(active)) button.setActive();
          sb.append(button.html());
        } catch (
          ClassNotFoundException | InstantiationException | IllegalAccessException e
        ) {
          e.printStackTrace();
        }
      }
    }
    sb.append(
      "<div style=\"float:right\"><a href=\"/api/logout\">Logout</a></div>\n</div>"
    );

    Message flash = Flash.message();
    if (flash != null) {
      sb.append(flash.getDiv());
    } else {
      sb.append(Flash.getEmptyDiv());
    }
    sb.append(
      "<script>setInterval(function(){$(\"#flash\").hide('slow');}, 10000);</script>"
    );
    return createPhraseNode(parent, "quoted", sb.toString());
  }
}
