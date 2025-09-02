package org.wingsofcarolina.quiz.extensions;

import java.util.Map;
import org.asciidoctor.ast.PhraseNode;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.InlineMacroProcessor;
import org.wingsofcarolina.quiz.common.Flash.Message;

public class Flash extends InlineMacroProcessor {

  public Flash(String macroName) {
    super(macroName);
  }

  @Override
  public PhraseNode process(
    StructuralNode parent,
    String target,
    Map<String, Object> attributes
  ) {
    StringBuffer sb = new StringBuffer();
    Message flash = org.wingsofcarolina.quiz.common.Flash.message();
    if (flash != null) {
      sb.append(flash.getDiv());
      sb.append(
        "<script>setInterval(function(){$(\"#flash\").hide('slow');}, 10000);</script>"
      );
      return createPhraseNode(parent, "quoted", sb.toString());
    } else {
      return null;
    }
  }
}
