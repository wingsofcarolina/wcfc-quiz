package org.wingsofcarolina.quiz.scripting;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.Map;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.resources.QuizContext;

import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;

public class Execute {
    private static int sequenceNumber = 0;
    private final static Logger LOG = LoggerFactory.getLogger(Execute.class);

    private final QuizContext context;
    private final CompilerConfiguration config = new CompilerConfiguration();

    public Execute(QuizContext context) {
        this.context = context;
        config.setScriptBaseClass("org.wingsofcarolina.quiz.scripting.QuizDSL");
    }

	public String run(String script) {
		return run(script, null);
	}
	
    public String run(String scriptText, Map<String, String> args) {
        return run(scriptText, null, args);
    }
    
    public String run(String scriptText, String name, Map<String, String> args) {
        String scriptName = name;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Binding binding = new Binding();
        GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding, config);

        if (scriptName == null) {
            scriptName = "QuizScript" + sequenceNumber++;
        }
        LOG.info("Invoking script named '{}' in GroovyShell", scriptName);
        
        StringReader reader = new StringReader(scriptText);
        GroovyCodeSource source = new GroovyCodeSource(reader, scriptName, "/groovy/shell");
        QuizDSL script = (QuizDSL) shell.parse(source);
        binding.setProperty("context", context);    // Decide if we want to give scripts access to the context
        if (args != null) {
        	binding.setProperty("args", args);
        }
        script.setContext(context);
        PrintStream origOut = System.out;
        PrintStream origErr = System.err;
        PrintStream newStream = new PrintStream(baos);
        binding.setProperty("out", newStream);
        binding.setProperty("err", newStream);
        System.setOut(newStream);
        System.setErr(newStream);
        try {
            script.run();
        } catch(Exception e) {
            LOG.info("Script execution error", e);
            throw e;
        } finally {
            System.setErr(origErr);
            System.setOut(origOut);
        }
        
        return baos.toString();
    }
}
