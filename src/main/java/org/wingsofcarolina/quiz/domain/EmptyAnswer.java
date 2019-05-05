package org.wingsofcarolina.quiz.domain;

public class EmptyAnswer extends Answer {

	public EmptyAnswer() {}

	public String getAnswer() {
		return "";
	}

	public boolean isCorrect() {
		return false;
	}
}
