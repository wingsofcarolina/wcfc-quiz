package org.wingsofcarolina.quiz.domain.presentation;

import java.util.List;

import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class CategoryReportWrapper extends QuestionListWrapper {

	private String category;

	public CategoryReportWrapper(User user, List<Question> questions, String category) {
		super(user, questions);
		this.category = category;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

}
