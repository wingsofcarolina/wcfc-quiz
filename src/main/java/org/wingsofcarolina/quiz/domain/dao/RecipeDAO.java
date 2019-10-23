package org.wingsofcarolina.quiz.domain.dao;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Recipe;
import org.wingsofcarolina.quiz.resources.Quiz;


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
	
	public Recipe getRecipeByCategoryAndAttribute(Category category, String attribute) {
		Recipe recipe = null;
		List<Recipe> result = getDatastore().find(Recipe.class).filter("category = ", category).asList();
		if (result.size() > 0) {
			if (result.size() == 1 || attribute == null) {
				recipe = result.get(0);
			} else {
				for (Recipe r : result) {
					if (r.getAttribute().toString().contentEquals(attribute.toUpperCase())) {
						recipe = r;
					}
				}
			}
		} 
		return recipe;
	}
	
	public void drop() {
		getDatastore().getCollection(Recipe.class).drop();
	}
}
