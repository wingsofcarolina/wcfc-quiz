package org.wingsofcarolina.quiz.domain.quiz;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.Category;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Recipe {
    @Id
	@JsonIgnore
    private ObjectId id;
    
    Category category;
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
		return "Recipe [category=" + category + ", sections=" + sections + "]";
	}
}
