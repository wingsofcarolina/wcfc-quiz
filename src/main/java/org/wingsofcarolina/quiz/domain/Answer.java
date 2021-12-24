package org.wingsofcarolina.quiz.domain;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.presentation.CommonMarkRenderer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itextpdf.layout.element.Paragraph;

public class Answer {
    @Id
	@JsonIgnore
    private ObjectId id;
	private String answer;
	private boolean correct;

	public Answer() {}
	
	public Answer(String answer, boolean correct) {
		this.answer = answer;
		this.correct = correct;
	}

	public Answer(String answer) {
		this(answer, false);
	}

	public ObjectId getId() {
		return id;
	}
	
	public String getAnswer() {
		return answer;
	}

	@JsonIgnore
	public String getAnswerAsHtml() {
		return CommonMarkRenderer.renderAsHtml(answer);
	}

	@JsonIgnore
	public Paragraph getAnswerAsIText() {
		return CommonMarkRenderer.renderToParagraph(answer);
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
		return "Answer [answer=" + answer + ", correct=" + correct + "]";
	}

	public int compareTo(Answer other) {
		if (! answer.equals(other.getAnswer())) return -1;
		if (correct != other.isCorrect()) return -1;
		return 0;
	}
}
