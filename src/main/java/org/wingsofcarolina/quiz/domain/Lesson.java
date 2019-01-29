package org.wingsofcarolina.quiz.domain;

import java.util.Date;

public class Lesson {
	public Date date;
	public String description;
	public String comment = null;
	
	// Default empty constructor needed for database
	public Lesson() {}
	
	public Lesson(String description) {
		this.description = description;
		this.date = new Date();
	}

	public Lesson(String description, String note) {
		this(description);
		this.comment = note;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public String toString() {
		return "Lesson [date=" + date + ", description=" + description + ", comment=" + comment + "]";
	}
}
