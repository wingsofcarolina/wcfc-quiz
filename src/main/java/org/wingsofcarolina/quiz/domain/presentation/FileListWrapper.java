package org.wingsofcarolina.quiz.domain.presentation;

import java.io.File;
import java.util.List;

import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class FileListWrapper {
	private User user;
	private File[] files;
	
	public FileListWrapper(User user, File[] files) {
		super();
		this.user = user;
		this.files = files;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public File[] getFiles() {
		return files;
	}
}
