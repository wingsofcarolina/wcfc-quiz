package org.wingsofcarolina.quiz.domain.presentation;

import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class QuestionWrapper {

  private User user;
  private Question question;

  public QuestionWrapper(User user) {
    this(user, null);
  }

  public QuestionWrapper(User user, Question question) {
    super();
    this.user = user;
    this.question = question;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Question getQuestion() {
    return question;
  }

  public void setQuestion(Question question) {
    this.question = question;
  }

  @Override
  public String toString() {
    return "QuestionWrapper [user=" + user + ", question=" + question + "]";
  }
}
