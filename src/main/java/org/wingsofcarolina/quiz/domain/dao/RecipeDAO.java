package org.wingsofcarolina.quiz.domain.dao;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Recipe;
import org.wingsofcarolina.quiz.domain.quiz.Quiz;


public class RecipeDAO extends BasicDAO<Recipe, ObjectId> {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(RecipeDAO.class);

	public RecipeDAO(Datastore ds) {
		super(Recipe.class, ds);
	}

	public List<Recipe> getAllRecipes() {
		List<Recipe> result = getDatastore().find(Recipe.class).asList();
		return result;
	}
	
	public Recipe getRecipeByType(Quiz.QuizType quizType) {
		List<Recipe> result = getDatastore().find(Recipe.class).filter("quizType = ", quizType).asList();
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}
}
