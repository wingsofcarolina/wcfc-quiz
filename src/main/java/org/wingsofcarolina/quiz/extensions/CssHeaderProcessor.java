package org.wingsofcarolina.quiz.extensions;

import java.util.Map;

import org.asciidoctor.ast.Document;
import org.asciidoctor.extension.DocinfoProcessor;

/**
 * Inject the link to the JQuery package into the header.
 * 
 * @author dwight
 *
 */
public class CssHeaderProcessor extends DocinfoProcessor { 

    public CssHeaderProcessor() {
        super();
    }

    public CssHeaderProcessor(Map<String, Object> config) {
        super(config);
    }

    @Override
    public String process(Document document) {
    	StringBuffer sb = new StringBuffer();
        sb.append("<script src=\"/static/jquery-3.4.0.js\"></script>\n");
        sb.append("<script src=\"/static/jquery.validate.js\"></script>");
        return sb.toString();
    }
}