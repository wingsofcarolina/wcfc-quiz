package org.wingsofcarolina.quiz.resources;

import java.util.HashMap;
import java.util.Map;

import org.wingsofcarolina.quiz.QuizConfiguration;

public class QuizContext {
	private final Quiz quiz;
    private final QuizConfiguration configuration;
    private Map<String,String> variables = new HashMap<String, String>();
	private boolean test = false;
    
    public QuizContext(Quiz quiz, QuizConfiguration configuration) {
    	this.quiz = quiz;
        this.configuration = configuration;
    }

    public Quiz getQuiz() {
    	return quiz;
    }
    
    public QuizConfiguration getConfiguration() {
        return configuration;
    }

	public Map<String, String> getVariables() {
		return variables;
	}

	public void setVariable(String name, String value) {
		variables.put(name, value);
	}
	
	public String getVariable(String name) {
		return variables.get(name);
	}

	public void setTest(boolean b) {
		this.test = b;
	}
	
	public boolean getTest() {
		return this.test;
	}
}
