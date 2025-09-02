package org.wingsofcarolina.quiz.extensions;

import java.util.Map;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;

public class Color extends InlineMacroProcessor {

  public Color(String macroName) {
    super(macroName);
  }

  @Override
  public PhraseNode process(
    StructuralNode parent,
    String target,
    Map<String, Object> attributes
  ) {
    StringBuffer sb = new StringBuffer();
    sb.append("<span style=\"color:" + target + "\">");
    sb.append(attributes.get("1"));
    sb.append("</span>");
    return createPhraseNode(parent, "quoted", sb.toString());
  }
}
