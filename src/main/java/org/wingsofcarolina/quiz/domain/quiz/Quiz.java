package org.wingsofcarolina.quiz.domain.quiz;

import java.util.ArrayList;
import java.util.List;

import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Recipe;
import org.wingsofcarolina.quiz.domain.Section;
import org.wingsofcarolina.quiz.domain.Selection;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;


public class Quiz {
	public enum QuizType { FAR, SOP_STUDENT, SOP_PILOT, SOP_INSTRUCTOR, C152, C172, PA28, M20J };

	private long quizId;
	private String quizName;
	private QuizType quizType;
	private Category category;
	private List<Question> questions = new ArrayList<Question>();
	
	public Quiz(String request) {
		this.quizId = Persistence.instance().generateAutoIncrement("quiz", 1000);
		switch (request) {
			case "far":
				category = Category.FAR;
				quizType = QuizType.FAR;
				quizName = "FAR 61/91";
				break;
			case "sop-student":
				category = Category.SOP;
				quizType = QuizType.SOP_STUDENT;
				quizName = "SOP - Student Pilot";
				break;
			case "sop-pilot":
				category = Category.SOP;
				quizType = QuizType.SOP_PILOT;
				quizName = "SOP - Licensed Pilot"; 
				break;
			case "sop-instructor":
				category = Category.SOP;
				quizType = QuizType.SOP_INSTRUCTOR;
				quizName = "SOP - Instructor";
				break;
			case "c152": category = Category.C152; quizType = QuizType.C152; quizName = "Cessna 152"; break;
			case "c172": category = Category.C172; quizType = QuizType.C172; quizName = "Cessna 172 Skyhawk"; break;
			case "pa28": category = Category.PA28; quizType = QuizType.PA28; quizName = "Piper PA-28 Warrior"; break;
			case "m20j": category = Category.M20J; quizType = QuizType.M20J; quizName = "Mooney M20J"; break;
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
	
	/**
	 * Based on a quiz recipe, build a quiz
	 * @return
	 */
	public Quiz build() {
		// Pick up the recipe for the desired quiz
		Recipe recipe = Recipe.getRecipeByType(quizType);

		// Iterate over all sections
		for (Section section : recipe.getSections()) {
			// Create a place to deposit all the candidate questions
			List<Question> candidates = new ArrayList<Question>();
			
			// Iterate all selections within the section
			for (Selection selection : section.getSelections()) {
				candidates = Question.getSelected(category, selection.getAttributes());
				// TODO: Eventually pick randomly, but for now add them all
				questions.addAll(candidates);
			}
		}
		
		// TODO: Pick-and-choose, rather than list all
		for (int i = 0; i < questions.size(); i++) {
			questions.get(i).setIndex(i + 1);
		}
		return this;
	}
}
