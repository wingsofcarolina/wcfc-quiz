package org.wingsofcarolina.quiz.domain.quiz;

import java.util.ArrayList;
import java.util.List;

import org.wingsofcarolina.quiz.domain.Attribute;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;

public class Quiz {

	private long quizId;
	private String quizName;
	private Category category;
	private List<String> attributes = new ArrayList<String>();
	private List<Question> questions = new ArrayList<Question>();
	
	public Quiz(String quizType) {
		this.quizId = Persistence.instance().generateAutoIncrement("quiz", 1000);
		switch (quizType) {
			case "far":
				category = Category.FAR;
				quizName = "FAR 61/91";
				break;
			case "sop-student":
				category = Category.SOP;
				quizName = "SOP - Student Pilot";
				this.attribute(Attribute.STUDENT);
				break;
			case "sop-pilot":
				category = Category.SOP;
				quizName = "SOP - Licensed Pilot"; 
				this.attribute(Attribute.PILOT);
				break;
			case "sop-instructor":
				category = Category.SOP;
				quizName = "SOP - Instructor";
				this.attribute(Attribute.INSTRUCTOR);
				break;
			case "c152": category = Category.C152; quizName = "Cessna 152"; break;
			case "c172": category = Category.C172; quizName = "Cessna 172 Skyhawk"; break;
			case "pa28": category = Category.PA28; quizName = "Piper PA-28 Warrior"; break;
			case "m20j": category = Category.M20J; quizName = "Mooney M20J"; break;
		}			
	}

	public long getQuizId() {
		return quizId;
	}
	
	public String getQuizName() {
		return quizName;
	}
	
	public Category getCategory() {
		return category;
	}
	
	public List<Question>getQuestions() {
		return questions;
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}
	
	public Quiz attribute(String attribute) {
		if (this.attributes == null) {
			this.attributes = new ArrayList<String>();
		}
		this.attributes.add(attribute);
		return this;
	}
	
	/**
	 * Based on a quiz recipe, build a quiz
	 * @return
	 */
	public Quiz build() {
		// Simulate a recipe retrieval
		if (attributes.size() == 0) {
			questions.addAll(Question.getSelected(category));
		} else {
			for (String attribute : attributes) {
				questions.addAll(Question.getSelected(category, attribute));
			}
		}
		
		// TODO: Pick-and-choose, rather than list all
		for (int i = 0; i < questions.size(); i++) {
			questions.get(i).setIndex(i + 1);
		}
		return this;
	}
}
