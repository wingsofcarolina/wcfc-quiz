package org.wingsofcarolina.quiz.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.dao.ExclusionGroupDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;

public class ExclusionGroup {

  @Id
  @JsonIgnore
  private ObjectId id;

  private Long groupId;
  private String name;
  private String description;
  private Integer count = 0;
  private Date createdDate = new Date();
  private List<Long> questionIds = new ArrayList<Long>();

  public static String ID_KEY = "group";

  public ExclusionGroup() {}

  public ExclusionGroup(String name, String description) {
    this.name = name;
    this.description = description;
    this.groupId = Persistence.instance().getID(ID_KEY, 1000);
  }

  public ExclusionGroup(String name) {
    this(name, null);
  }

  public Long getGroupId() {
    return groupId;
  }

  public void setGroupId(Long groupId) {
    this.groupId = groupId;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getCreatedDate() {
    return createdDate;
  }

  public List<Long> getQuestionIds() {
    return questionIds;
  }

  public void addQuestionId(Long questionId) {
    if (!this.questionIds.contains(questionId)) {
      this.questionIds.add(questionId);
    }
    this.count = questionIds.size();
  }

  public void removeGroupId(Long questionId) {
    this.questionIds.remove(questionId);
    this.count = questionIds.size();
  }

  @Override
  public String toString() {
    return (
      "ExclusionGroup [groupId=" +
      groupId +
      ", groupName=" +
      name +
      ", createdDate=" +
      createdDate +
      ", questionIds=" +
      questionIds +
      "]"
    );
  }

  /*
   * Database Management Functionality
   */
  public static List<ExclusionGroup> getAllGroups() {
    ExclusionGroupDAO exclusionGroupDAO = (ExclusionGroupDAO) Persistence
      .instance()
      .get(ExclusionGroup.class);
    return exclusionGroupDAO.getAllGroups();
  }

  public static ExclusionGroup getByGroupId(Long id) {
    ExclusionGroupDAO exclusionGroupDAO = (ExclusionGroupDAO) Persistence
      .instance()
      .get(ExclusionGroup.class);
    return exclusionGroupDAO.getByGroupId(id);
  }

  public static Object getByName(String name) {
    ExclusionGroupDAO exclusionGroupDAO = (ExclusionGroupDAO) Persistence
      .instance()
      .get(ExclusionGroup.class);
    return exclusionGroupDAO.getByName(name);
  }

  @SuppressWarnings("unchecked")
  public void save() {
    Persistence.instance().get(Question.class).save(this);
  }

  @SuppressWarnings("unchecked")
  public void delete() {
    Persistence.instance().get(Question.class).delete(this);
  }
}
