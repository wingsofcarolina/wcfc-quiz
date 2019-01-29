package org.wingsofcarolina.quiz.domain.presentation;

import org.wingsofcarolina.quiz.common.FlashMessage;
import org.wingsofcarolina.quiz.domain.User;

public class Wrapper {
	private boolean isInstructor;
	private boolean canEdit;
	private User requester;
	private User user;
	
	public Wrapper(User requester, User user) {
		this.requester = requester;
		this.user = user;
		this.canEdit = isOwner() || user.hasInstructor(requester);
		this.isInstructor = requester.isInstructor();
	}

	public boolean isOwner() {
		return requester.getUserId().equals(user.getUserId());
	}
	
	public boolean isInstructor() {
		return isInstructor;
	}

	public boolean canEdit() {
		return canEdit;
	}
	
	public User getUser() {
		return user;
	}

	public User getRequester() {
		return requester;
	}
	
	public String message() {
		String msg = FlashMessage.message();
		if (msg != null) {
			return msg;
		}
		return user.getMessage();
	}
}
