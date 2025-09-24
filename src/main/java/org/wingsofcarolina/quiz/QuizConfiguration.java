package org.wingsofcarolina.quiz;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.core.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class QuizConfiguration extends Configuration {

  @JsonProperty
  String mongodb;

  @JsonProperty
  String templates;

  @JsonProperty
  String dataDirectory;

  @JsonProperty
  String imageDirectory;

  @JsonProperty
  String imageRoot;

  @JsonProperty
  String assetDirectory;

  @JsonProperty
  String mode;

  @JsonProperty
  String slackWebhookUrl;

  @JsonProperty
  String adminEmail;

  @JsonProperty
  String adminName;

  @JsonProperty
  String adminPassword;

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

  public String getDataDirectory() {
    return dataDirectory;
  }

  public String getImageDirectory() {
    return imageDirectory;
  }

  public String getImageRoot() {
    return imageRoot;
  }

  public String getMode() {
    return mode;
  }

  public String getSlackWebhookUrl() {
    return slackWebhookUrl;
  }

  public String getAssetDirectory() {
    return assetDirectory;
  }

  public String getAdminEmail() {
    return adminEmail;
  }

  public String getAdminName() {
    return adminName;
  }

  public String getAdminPassword() {
    return adminPassword;
  }
}
