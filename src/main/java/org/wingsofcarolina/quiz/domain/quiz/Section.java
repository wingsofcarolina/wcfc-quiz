package org.wingsofcarolina.quiz.domain.quiz;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Section {
    @Id
	@JsonIgnore
    private ObjectId id;
    
    String name;
    List<Selection> selections;
    
    public Section() {}
    
    public Section(String name) {
    	this.name = name;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Selection> getSelections() {
		return selections;
	}

	public void setSelections(List<Selection> selections) {
		this.selections = selections;
	}

	public void addSelection(Selection selection) {
		if (selections == null) {
			this.selections = new ArrayList<Selection>();
		}
		this.selections.add(selection);
	}

	@Override
	public String toString() {
		return "Section [name=" + name + ", selections=" + selections + "]";
	}
}
