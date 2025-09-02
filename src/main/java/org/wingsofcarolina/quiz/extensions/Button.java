package org.wingsofcarolina.quiz.extensions;

import java.util.Map;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class Button extends InlineMacroProcessor {

  public Button(String macroName) {
    super(macroName);
  }

  @Override
  public PhraseNode process(
    StructuralNode parent,
    String url,
    Map<String, Object> attributes
  ) {
    String method = (String) attributes.get("method");
    String target = (String) attributes.get("target");
    if (method == null) {
      method = "post";
    }
    StringBuffer sb = new StringBuffer();
    if (target == null) {
      sb.append("<form action=\"" + url + "\" method=\"" + method + "\">");
    } else {
      sb.append(
        "<form action=\"" +
        url +
        "\" method=\"" +
        method +
        "\" target=\"" +
        target +
        "\">"
      );
    }
    sb.append("<input type=\"submit\" value=\"" + attributes.get("label") + "\" />");
    sb.append("</form>");
    return createPhraseNode(parent, "quoted", sb.toString());
  }
}
