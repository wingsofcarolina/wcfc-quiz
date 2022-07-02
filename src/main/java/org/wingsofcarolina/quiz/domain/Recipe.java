package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;
import org.wingsofcarolina.quiz.domain.dao.RecipeDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.scripting.Section;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Recipe {
    @Id
	@JsonIgnore
    private ObjectId id;
    
    String name;
    String alias;
    Integer order;
    Long recipeId;
    String script = null;
    Category category;
    String attribute;
    @Transient
	@JsonIgnore
    List<Section> sections;
    
	public static String ID_KEY = "recipe";

    public Recipe() {
		this.recipeId = Persistence.instance().getID(ID_KEY, 1000);
    }
    
	public Long getRecipeId() {
		return recipeId;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getScript() {
		return script;
	}

	public void setScript(String script) {
		this.script = script;
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
		return "Recipe [name=" + name + ", recipeId=" + recipeId + ", script=" + script + "]";
	}

	/*
	 * Database Management Functionality
	 */
	public static void drop() {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		recipeDao.drop();
	}
	
	public static List<Recipe> getAllRecipes() {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		return recipeDao.getAllRecipes();
	}

	public static Recipe getRecipeById(Long recipeId) {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		return recipeDao.getRecipeById(recipeId);
	}
	
	public static Recipe getRecipeByAlias(String alias) {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		return recipeDao.getRecipeByAlias(alias);
	}
	
	public static Recipe getRecipe(String name) {
		RecipeDAO recipeDao = (RecipeDAO) Persistence.instance().get(Recipe.class);
		return recipeDao.getRecipe(name);
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
