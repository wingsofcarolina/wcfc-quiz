package org.wingsofcarolina.quiz.domain.presentation;

import java.util.List;

import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class AttributeReportWrapper extends QuestionListWrapper {

	private String attribute;

	public AttributeReportWrapper(User user, List<Question> questions, String attribute) {
		super(user, questions);
		this.attribute = attribute;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

}
