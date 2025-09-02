package org.wingsofcarolina.quiz.domain.presentation;

import static org.asciidoctor.Asciidoctor.Factory.create;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.extension.JavaExtensionRegistry;
import org.wingsofcarolina.quiz.QuizConfiguration;
import org.wingsofcarolina.quiz.extensions.Button;
import org.wingsofcarolina.quiz.extensions.Color;
import org.wingsofcarolina.quiz.extensions.CssHeaderProcessor;
import org.wingsofcarolina.quiz.extensions.Flash;
import org.wingsofcarolina.quiz.extensions.NavigationBar;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

public class Renderer {

	@SuppressWarnings("unused")
	private QuizConfiguration config;
	private Asciidoctor asciidoctor;
	private Options options;
	private Configuration freemarker;

	public Renderer(QuizConfiguration config) throws IOException {
		this.config = config;
		
		Map<String, Object> userAttributes = new HashMap<String,Object>();
		userAttributes.put("quiz-version","1.2.0");

		asciidoctor = create();
		Attributes attributes = Attributes.builder()
				.linkCss(true)
				.noFooter(true)
				.styleSheetName("/static/default.css")
				.allowUriRead(true)
				.attribute("quiz-version", "1.2.0")
				.build();
		options = Options.builder()
				.safe(SafeMode.SERVER)
				.inPlace(true)
				.backend("html5")
				.standalone(true)
				.attributes(attributes)
				.build();
		
		// Add custom extensions
		JavaExtensionRegistry extensionRegistry = this.asciidoctor.javaExtensionRegistry(); 
		extensionRegistry.docinfoProcessor(new CssHeaderProcessor(new HashMap<String, Object>())); 
		extensionRegistry.inlineMacro("navbar", NavigationBar.class);
		extensionRegistry.inlineMacro("flash", Flash.class);
		extensionRegistry.inlineMacro("button", Button.class);
		extensionRegistry.inlineMacro("color", Color.class);

		// Create your Configuration instance, and specify if up to what FreeMarker
		// version (here 2.3.27) do you want to apply the fixes that are not 100%
		// backward-compatible. See the Configuration JavaDoc for details.
		freemarker = new Configuration(Configuration.VERSION_2_3_22);

		// Specify the source where the template files come from. Here I set a
		// plain directory for it, but non-file-system sources are possible too:
		// TODO: Make this a configuration option
		freemarker.setDirectoryForTemplateLoading(new File(config.getTemplates()));
		freemarker.setTemplateUpdateDelay(0);  // TODO: Change this for "production"

		// Set the preferred charset template files are stored in. UTF-8 is
		// a good choice in most applications:
		freemarker.setDefaultEncoding("UTF-8");

		// Sets how errors will appear.
		// During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
		freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

		// Don't log exceptions inside FreeMarker that it will thrown at you anyway:
		freemarker.setLogTemplateExceptions(false);
	}
	
	public String render(String template, Object wrapper) throws IOException {
		String output = null;
		
		String rendered = renderFreemarker(template, wrapper).toString();
		if (template.endsWith(".ad") ) {
			output = asciidoctor.convert(rendered, options);
		} else {
			output = rendered;
		}
		return output;
	}
	
	private Writer renderFreemarker(String template, Object entity) throws IOException {
		Template temp = freemarker.getTemplate(template);

		Writer out = new StringWriter();
		try {
			temp.process(entity, out);
		} catch (TemplateException e) {
			e.printStackTrace();
		}
		
		return out;
	}
}
