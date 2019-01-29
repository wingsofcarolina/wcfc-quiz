package org.wingsofcarolina.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class QuizConfiguration extends Configuration {
	@JsonProperty String mongodb;
	@JsonProperty String templates;

	public String getMongodb() {
		return mongodb;
	}

	public void setMongodb(String mongodb) {
		this.mongodb = mongodb;
	}

	public String getTemplates() {
		return templates;
	}

	public void setTemplates(String templates) {
		this.templates = templates;
	}
}
