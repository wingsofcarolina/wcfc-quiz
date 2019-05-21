package org.wingsofcarolina.quiz.domain.presentation;

import org.wingsofcarolina.quiz.domain.User;

public class JsonWrapper {
	private String json;
	private User user;
	
	public JsonWrapper(User user, String json) {
		this.json = json;
		this.user = user;
	}

	public User getUser() {
		return user;
	}
	public String getJson() {
		return json;
	}
}
