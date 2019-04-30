package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.dao.RecipeDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.resources.Quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Recipe {
    @Id
	@JsonIgnore
    private ObjectId id;
    
    Quiz.QuizType quizType;
    List<Section> sections;
    
    public Recipe() {}

	public Recipe(Quiz.QuizType quizType) {
		this.quizType = quizType;
	}

	public Quiz.QuizType getQuizType() {
		return quizType;
	}

	public void setQuizType(Quiz.QuizType quizType) {
		this.quizType = quizType;
	}

	public List<Section> getSections() {
		return sections;
	}

	public void setSections(List<Section> sections) {
		this.sections = sections;
	}

	public void addSection(Section Section) {
		if (sections == null) {
			this.sections = new ArrayList<Section>();
		}
		this.sections.add(Section);
	}

	@Override
	public String toString() {
		return "Recipe [quizType=" + quizType + ", sections=" + sections + "]";
	}

	/*
	 * Database Management Functionality
	 */
	public static List<Recipe> getAllRecipes() {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		return recipeDao.getAllRecipes();
	}

	public static Recipe getRecipeByType(Quiz.QuizType quizType) {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		return recipeDao.getRecipeByType(quizType);
	}
	
	@SuppressWarnings("unchecked")
	public void save() {
		Persistence.instance().get(Recipe.class).save(this);
	}
	
	@SuppressWarnings("unchecked")
	public void delete() {
		Persistence.instance().get(Recipe.class).delete(this);
	}

}
