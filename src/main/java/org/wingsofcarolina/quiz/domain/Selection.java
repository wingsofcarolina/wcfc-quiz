package org.wingsofcarolina.quiz.domain;

import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Selection {
    @Id
	@JsonIgnore
    private ObjectId id;
    
    Integer count;
	private List<String> attributes;
	
	public Selection() {}
	
	public Selection(Integer count, List<String> attributes) {
		this.count = count;
		this.attributes = attributes;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}

	@Override
	public String toString() {
		return "Selection [count=" + count + ", attributes=" + attributes + "]";
	}
}
