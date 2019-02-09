package org.wingsofcarolina.quiz.domain.presentation;

import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.domain.User;

public class Wrapper {
	private User requester;
	
	public Wrapper(User requester) {
		this.requester = requester;
	}
	
	public boolean isAdmin() {
		return requester.getPrivileges().contains(Privilege.ADMIN);
	}

	public User getRequester() {
		return requester;
	}
}
