package org.wingsofcarolina.quiz.domain.presentation;

import java.net.URI;

import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.domain.User;

public class RecipeWrapper {
	private User user;
	private URI url;
	
	public RecipeWrapper(User user, URI url) {
		this.user = user;
		this.url = url;
	}
	
	public boolean isAdmin() {
		return user.getPrivileges().contains(Privilege.ADMIN);
	}

	public User getUser() {
		return user;
	}

	public URI getUrl() {
		return url;
	}

	@Override
	public String toString() {
		return "RecipeWrapper [user=" + user + ", url=" + url + "]";
	}
}
