package org.wingsofcarolina.quiz.domain.presentation;

public class QuizBuildErrorWrapper {

  private String message;
  private String output;

  public QuizBuildErrorWrapper(String message, String output) {
    this.message = message;
    this.output = output;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getOutput() {
    return output;
  }

  public void setOutput(String output) {
    this.output = output;
  }
}
