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
    
    Category category;
    String attribute;
    List<Section> sections;
    
    public Recipe() {}

	public Recipe(Category category) {
		this.category = category;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
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
		if (attribute != null) {
			return "Recipe [category=" + category + ", attribute=" + attribute + ", sections=" + sections + "]";
		} else {
			return "Recipe [category=" + category + ", sections=" + sections + "]";
		}
	}

	/*
	 * Database Management Functionality
	 */
	public static List<Recipe> getAllRecipes() {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		return recipeDao.getAllRecipes();
	}

	public static Recipe getRecipeByCategoryAndAttribute(Category category, String attribute) {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		return recipeDao.getRecipeByCategoryAndAttribute(category, attribute);
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
