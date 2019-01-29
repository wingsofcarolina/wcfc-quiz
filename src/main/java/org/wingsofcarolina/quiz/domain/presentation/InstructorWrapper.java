package org.wingsofcarolina.quiz.domain.presentation;

import java.util.ArrayList;
import java.util.List;

import org.wingsofcarolina.quiz.domain.User;

public class InstructorWrapper extends Wrapper {

	private List<User> students = new ArrayList<User>();
	
	public InstructorWrapper(User requester, User user) {
		super(requester, user);
		if (user.hasStudents()) {
			for (String userId : user.getStudents()) {
				User student = User.getByUserId(userId);
				students.add(student);
			}
		}
	}

	public List<User> getStudents() {
		return students;
	}

	public void setStudents(List<User> students) {
		this.students = students;
	}
}
