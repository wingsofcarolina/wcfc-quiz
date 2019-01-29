package org.wingsofcarolina.quiz.domain.presentation;

import java.util.List;

import org.wingsofcarolina.quiz.domain.User;

public class AddInstructorWrapper {
	private User requester;
	private List<User> instructors;
	
	public AddInstructorWrapper(User requester, List<User> instructors) {
		this.requester = requester;
		this.instructors = instructors;
	}

	public User getRequester() {
		return requester;
	}

	public List<User> getInstructors() {
		return instructors;
	}
}
