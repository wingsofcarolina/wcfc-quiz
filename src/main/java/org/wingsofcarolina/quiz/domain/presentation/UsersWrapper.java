package org.wingsofcarolina.quiz.domain.presentation;

import java.util.List;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class UsersWrapper {

  private User user;
  private List<User> users;

  public UsersWrapper(User user) {
    this(user, null);
  }

  public UsersWrapper(User user, List<User> users) {
    super();
    this.user = user;
    this.users = users;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public List<User> getUsers() {
    return users;
  }

  public void setUsers(List<User> users) {
    this.users = users;
  }

  @Override
  public String toString() {
    return "UsersWrapper [user=" + user + ", users=" + users + "]";
  }
}
