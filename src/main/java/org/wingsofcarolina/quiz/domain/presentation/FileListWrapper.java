package org.wingsofcarolina.quiz.domain.presentation;

import java.io.File;
import java.util.List;

import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class FileListWrapper {
	private User user;
	private String root;
	private File[] files;
	
	public FileListWrapper(User user, String root, File[] files) {
		super();
		this.user = user;
		this.root = root;
		this.files = files;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getRoot() {
		return root;
	}
	
	public File[] getFiles() {
		return files;
	}
}
