package org.wingsofcarolina.quiz.scripting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.Question;

public class Section {

  @Id
  @JsonIgnore
  private ObjectId id;

  String name;
  Integer count;
  List<Question> required;
  List<Question> selections;

  public Section() {}

  public Section(Integer count, String name) {
    this.count = count;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getCount() {
    return count;
  }

  public void setCount(Integer count) {
    this.count = count;
  }

  public List<Question> getRequired() {
    return required;
  }

  public void setRequired(List<Question> required) {
    this.required = required;
  }

  public void addRequired(Question question) {
    if (required == null) {
      required = new ArrayList<Question>();
    }
    required.add(question);
  }

  public List<Question> getSelections() {
    return selections;
  }

  public void setSelections(List<Question> selections) {
    this.selections = selections;
  }

  public void addSelection(Question question) {
    if (selections == null) {
      selections = new ArrayList<Question>();
    }
    selections.add(question);
  }

  // Return a randomized list of questions ensuring that the
  // required questions are included.
  public List<Question> getQuestions() {
    List<Question> result = new ArrayList<Question>();

    if (required != null) {
      result.addAll(required);
    }

    if (selections != null) {
      int max = (count - result.size() > selections.size())
        ? selections.size()
        : count - result.size();
      for (int i = 0; i < max; i++) {
        result.add(selections.get(i));
      }
    } else {
      if (selections != null) {
        result.addAll(selections);
      }
    }

    return randomize(result);
  }

  public List<Question> randomize(List<Question> pool) {
    List<Question> result = new ArrayList<Question>();

    if (pool.size() > 0) {
      do {
        int size = pool.size();
        if (size > 0) {
          int pick = (int) (Math.random() * size);
          result.add(pool.remove(pick));
        }
      } while (pool.size() > 0);
    }

    return result;
  }

  public boolean isFull() {
    int r = required == null ? 0 : required.size();
    int s = selections == null ? 0 : selections.size();

    if (r + s > count) return true; else return false;
  }

  public boolean hasQuestion(Question candidate) {
    long id = candidate.getQuestionId();
    if (required != null) {
      for (Question question : required) {
        if (question.getQuestionId() == id) {
          return true;
        }
      }
    }
    if (selections != null) {
      for (Question question : selections) {
        if (question.getQuestionId() == id) {
          return true;
        }
      }
    }
    return false;
  }
}
