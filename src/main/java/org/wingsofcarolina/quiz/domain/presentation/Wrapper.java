package org.wingsofcarolina.quiz.domain.presentation;

import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.domain.User;

public class Wrapper {
	private User user;
	
	public Wrapper(User user) {
		this.user = user;
	}
	
	public boolean isAdmin() {
		return user.getPrivileges().contains(Privilege.ADMIN);
	}

	public User getUser() {
		return user;
	}
}
