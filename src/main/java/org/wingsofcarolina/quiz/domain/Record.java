package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mongodb.morphia.annotations.Reference;

public class Record {
	@Reference
	private String ownerId;
	@Reference
	private List<User> instructors = new ArrayList<User>();
	private Syllabus syllabus;
	private Map<Integer, Task> tasks = null;
	private List<Lesson> lessons = null;
	private Type type;

	// Default empty constructor needed for database
	public Record() {}
	
	public Record(Type type, User instructor, Syllabus syllabus) {
		this.type = type;
		this.instructors.add(instructor);
		this.syllabus = syllabus;
		syllabus.initialize(this);
	}
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	public Type getType() {
		return type;
	}
	public void setType(Type type) {
		this.type = type;
	}
	public List<User> getInstructors() {
		return instructors;
	}
	public void addInstructor(User instructor) {
		this.instructors.add(instructor);
	}
	public Syllabus getSyllabus() {
		return syllabus;
	}
	public void setSyllabus(Syllabus syllabus) {
		this.syllabus = syllabus;
	}
	public List<Lesson> getLessons() {
		return lessons;
	}
	public List<Task> getTasks() {
		return new ArrayList<Task>(tasks.values());
	}
	public Task getTask(Integer index) {
		return tasks.get(index);
	}
	public void addLesson(Lesson lesson) {
		if (lessons == null) {
			lessons = new ArrayList<Lesson>();
		}
		lessons.add(lesson);
	}
	public void addTask(Integer index, String description) {
		if (tasks == null) {
			tasks = new HashMap<Integer, Task>();
		}
		tasks.put(index, new Task(index, description));
	}

	@Override
	public String toString() {
		return "Record [ownerId=" + ownerId + ", instructors=" + instructors + ", type=" + type + ", lessonCount = " + lessons.size() + "]";
	}
}
