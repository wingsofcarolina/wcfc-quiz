package org.wingsofcarolina.quiz.domain.presentation;

import java.util.Map;

public class Version {
	private Map<String, Object> data;

	public Version(Map<String, Object> data) {
		super();
		this.data = data;
	}

	public String getVersion() {
		return (String)data.get("git.build.version");
	}

	public String getCommit() {
		return (String)data.get("git.commit.id.abbrev");
	}
}
