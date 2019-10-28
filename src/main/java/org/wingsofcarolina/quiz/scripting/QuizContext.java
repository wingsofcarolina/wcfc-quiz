package org.wingsofcarolina.quiz.scripting;

import org.wingsofcarolina.quiz.QuizConfiguration;

public class QuizContext {
    private final QuizConfiguration configuration;
    
    public QuizContext(QuizConfiguration configuration) {
        this.configuration = configuration;
    }

    public QuizConfiguration getConfiguration() {
        return configuration;
    }
}
