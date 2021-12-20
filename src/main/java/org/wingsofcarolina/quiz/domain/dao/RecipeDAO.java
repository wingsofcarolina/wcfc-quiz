package org.wingsofcarolina.quiz.domain.dao;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Recipe;


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

	public Recipe getRecipe(String name) {
		Recipe recipe = null;
		List<Recipe> result = getDatastore().find(Recipe.class).filter("name = ", name.toUpperCase()).asList();
		if (result != null && result.size() > 0) {
			recipe = result.get(0);
		} 
		return recipe;
	}

	public Recipe getRecipeById(Long recipeId) {
		Recipe recipe = null;
		List<Recipe> result = getDatastore().find(Recipe.class).filter("recipeId = ", recipeId).asList();
		if (result != null && result.size() > 0) {
			recipe = result.get(0);
		} 
		return recipe;
	}

	public void drop() {
		getDatastore().getCollection(Recipe.class).drop();
	}
}
