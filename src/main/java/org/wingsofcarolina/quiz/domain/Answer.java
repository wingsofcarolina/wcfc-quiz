package org.wingsofcarolina.quiz.domain;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.presentation.CmRenderer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itextpdf.layout.element.IBlockElement;
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
		return CmRenderer.renderAsHtml(answer);
	}

	@JsonIgnore
	public Paragraph getAnswerAsIText() {
		return CmRenderer.renderToParagraph(answer);
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
}
