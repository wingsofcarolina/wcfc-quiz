package org.wingsofcarolina.quiz;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.knowm.dropwizard.sundial.SundialConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class QuizConfiguration extends Configuration {
	@JsonProperty String mongodb;
	@JsonProperty String templates;
	@JsonProperty String dataDirectory;
	
	@Valid
	@NotNull
	public SundialConfiguration sundialConfiguration = new SundialConfiguration();
	
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

	@JsonProperty("sundial")
	public SundialConfiguration getSundialConfiguration() {

	  return sundialConfiguration;
	}

	public String getDataDirectory() {
		return dataDirectory;
	}

	public void setDataDirectory(String dataDirectory) {
		this.dataDirectory = dataDirectory;
	}
}
