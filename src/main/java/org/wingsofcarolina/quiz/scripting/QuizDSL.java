package org.wingsofcarolina.quiz.scripting;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import groovy.lang.Script;

public abstract class QuizDSL extends Script {

    private QuizContext context;
    
    public QuizContext getContext() {
        return context;
    }

    public void setContext(QuizContext context) {
        this.context = context;
    }

    // Implement classic Groovy method/property missing behavior
    public Object propertyMissing(String name) {
        return "Access property " + name;
    }
    public Object methodMissing(String name, Object args) {
        List<Object> argsList = Arrays.asList((Object[]) args);
        System.out.println("methodMissing called for ---> " + name);
        return "methodMissing called with name '" + name + "' and args = " + argsList;
    }
    
    // Methods supporting the Prefect DSL functions
    public void hello_script() {
        System.out.println("Hello Scripting!");
    }
}
