package org.wingsofcarolina.quiz.domain;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Answer {
    @Id
	@JsonIgnore
    private ObjectId id;
	private Integer index;
	private String answer;
	private boolean correct;

	public Answer() {}
	
	public Answer(Integer index, String answer, boolean correct) {
		this.index = index;
		this.answer = answer;
		this.correct = correct;
	}

	public Answer(Integer index, String answer) {
		this(index, answer, false);
	}

	public Answer(String answer) {
		this(-1, answer, false);
	}

	public ObjectId getId() {
		return id;
	}
	
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public boolean isCorrect() {
		return correct;
	}

	public void setCorrect(boolean correct) {
		this.correct = correct;
	}

	@Override
	public String toString() {
		return "Answer [index=" + index + ", answer=" + answer + "]";
	}
}
