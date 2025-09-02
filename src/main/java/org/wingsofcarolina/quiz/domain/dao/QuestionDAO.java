package org.wingsofcarolina.quiz.domain.dao;

import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Type;

public class QuestionDAO extends BasicDAO<Question, ObjectId> {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(QuestionDAO.class);

  public QuestionDAO(Datastore ds) {
    super(Question.class, ds);
  }

  public List<Question> getAllQuestions() {
    List<Question> result = getDatastore()
      .find(Question.class)
      .order("questionId")
      .asList();
    return result;
  }

  public List<Question> getByCategory(Category category) {
    List<Question> result = getDatastore()
      .find(Question.class)
      .filter("category = ", category)
      .order("questionId")
      .asList();
    return result;
  }

  public List<Question> getQuestionsLimited(int skip, int count) {
    List<Question> result = getDatastore()
      .find(Question.class)
      .order("questionId")
      .asList(new FindOptions().skip(skip).limit(10));
    return result;
  }

  public Question getByQuestionId(Long questionId) {
    List<Question> result = getDatastore()
      .find(Question.class)
      .filter("questionId = ", questionId)
      .order("questionId")
      .asList();
    if (result.size() > 0) {
      return result.get(0);
    } else {
      return null;
    }
  }

  public List<Question> getByType(Type type) {
    List<Question> result = getDatastore()
      .find(Question.class)
      .filter("type = ", type)
      .order("questionId")
      .asList();
    return result;
  }

  public List<Question> getRequiredWithAll(List<String> attributes) {
    if (attributes != null) {
      Query<Question> query = getDatastore()
        .createQuery(Question.class)
        .disableValidation();
      query.field("attributes").hasAllOf(attributes).field("required").equal(true).get();
      List<Question> result = query.order("questionid").asList();

      return result;
    } else {
      return null;
    }
  }

  public List<Question> getSelectedWith(String attribute) {
    if (attribute != null) {
      Query<Question> query = getDatastore()
        .createQuery(Question.class)
        .disableValidation();

      query.filter("attributes = ", attribute);
      query.get();

      List<Question> result = query.order("questionid").asList();
      return result;
    } else {
      return null;
    }
  }

  public List<Question> getWithAll(List<String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return null;
    } else {
      Query<Question> query = getDatastore()
        .createQuery(Question.class)
        .disableValidation();

      query.field("attributes").hasAllOf(attributes).get();

      List<Question> result = query.order("questionid").asList();
      return result;
    }
  }

  public List<Question> getCategoryWithAll(String category, List<String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return null;
    } else {
      Query<Question> query = getDatastore()
        .createQuery(Question.class)
        .disableValidation();

      query.field("category").equalIgnoreCase(category);
      query.field("attributes").hasAllOf(attributes).get();

      List<Question> result = query.order("questionid").asList();
      return result;
    }
  }

  public List<Question> getWithAny(List<String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return null;
    } else {
      Query<Question> query = getDatastore()
        .createQuery(Question.class)
        .disableValidation();

      query.field("attributes").hasAnyOf(attributes).get();

      List<Question> result = query.order("questionid").asList();
      return result;
    }
  }

  public List<Question> getCategoryWithAny(String category, List<String> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      return null;
    } else {
      Query<Question> query = getDatastore()
        .createQuery(Question.class)
        .disableValidation();

      query.field("category").equalIgnoreCase(category);
      query.field("attributes").hasAnyOf(attributes).get();

      List<Question> result = query.order("questionid").asList();
      return result;
    }
  }

  public List<Question> getAllQuarantined() {
    List<Question> result = getDatastore()
      .find(Question.class)
      .filter("quarantined = ", true)
      .asList();
    return result;
  }

  public List<Question> getSuperseded() {
    List<Question> result = getDatastore()
      .find(Question.class)
      .filter("supersededBy != ", -1)
      .asList();
    return result;
  }

  public void drop() {
    getDatastore().getCollection(Question.class).drop();
  }
}
