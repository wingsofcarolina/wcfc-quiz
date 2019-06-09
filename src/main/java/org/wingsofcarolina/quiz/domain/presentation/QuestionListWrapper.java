package org.wingsofcarolina.quiz.domain.presentation;

import java.util.List;

import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class QuestionListWrapper {
	private User user;
	private List<Question> questions;
	Integer index;
	Integer count;
	
	public QuestionListWrapper(User user, List<Question> questions, Integer index, Integer count) {
		super();
		this.user = user;
		this.questions = questions;
		this.index = index;
		this.count = count;
	}

	public QuestionListWrapper(User user, List<Question> questions) {
		super();
		this.user = user;
		this.questions = questions;
		this.index = 0;
		this.count = questions.size();
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<Question> getQuestions() {
		return questions;
	}

	public void setQuestions(List<Question> questions) {
		this.questions = questions;
	}
	
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public Integer skip() {
		return index + count;
	}
	
	public Integer skipBack() {
		int skip = index - count;
		skip = skip < 0 ? 0 : skip;
		return skip;
	}
	
	@Override
	public String toString() {
		return "QuestionWrapper [user=" + user + ", questions=" + questions + "]";
	}
}
