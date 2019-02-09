package org.wingsofcarolina.quiz.domain.presentation;

import java.util.List;

import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class QuizWrapper extends Wrapper {

	private String quizName;
	private List<Question>questions;
	
	public QuizWrapper(User requester) {
		super(requester);
	}
	
	public QuizWrapper(User requester, String quizName) {
		this(requester);
		this.quizName = quizName;
		questions = Question.getAllQuestions();
	}

	public String getQuizName() {
		return quizName;
	}
	
	public List<Question>getQuestions() {
		return questions;
	}
}
