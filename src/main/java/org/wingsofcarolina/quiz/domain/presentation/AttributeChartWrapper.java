package org.wingsofcarolina.quiz.domain.presentation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class AttributeChartWrapper extends QuestionListWrapper {

  private String attribute;
  private AttributeStats stats;
  private Integer count;
  private Integer easy = 0;
  private Integer medium = 0;
  private Integer hard = 0;

  public AttributeChartWrapper(User user, List<Question> questions, String attribute) {
    super(user, questions);
    this.attribute = attribute;
    this.stats = new AttributeStats(questions);
    this.easy = stats.getEasy();
    this.medium = stats.getMedium();
    this.hard = stats.getHard();
    this.count = questions.size();

    int count = 0;
    int last = stats.attributes.size() - 1;
    for (ColumnStats stat : stats.attributes.values()) {
      if (count == last) {
        stat.setLast();
      }
      count++;
    }
  }

  public Integer getCount() {
    return count;
  }

  public Integer getEasy() {
    return easy;
  }

  public Integer getMedium() {
    return medium;
  }

  public Integer getHard() {
    return hard;
  }

  public String getAttribute() {
    return attribute;
  }

  public AttributeStats getStats() {
    return stats;
  }

  public class AttributeStats {

    private Integer easy = 0;
    private Integer medium = 0;
    private Integer hard = 0;
    Map<String, ColumnStats> attributes = new TreeMap<String, ColumnStats>();

    public AttributeStats(List<Question> questions) {
      for (Question question : questions) {
        for (String attribute : question.getAttributes()) {
          if (attribute != null) {
            switch (attribute) {
              case "EASY":
                easy++;
                break;
              case "MEDIUM":
                medium++;
                break;
              case "HARD":
                hard++;
                break;
              default:
                if (attributes.containsKey(attribute)) {
                  attributes.get(attribute).increment(question.getAttributes());
                } else {
                  attributes.put(
                    attribute,
                    new ColumnStats(attribute, question.getAttributes())
                  );
                }
            }
          }
        }
      }
    }

    public Integer getEasy() {
      return easy;
    }

    public Integer getMedium() {
      return medium;
    }

    public Integer getHard() {
      return hard;
    }

    public Collection<ColumnStats> getAttributes() {
      return attributes.values();
    }
  }

  public class ColumnStats {

    private String name;
    private Integer easy = 0;
    private Integer medium = 0;
    private Integer hard = 0;
    private Integer other = 0;
    private boolean last = false;

    public ColumnStats(String name, List<String> attributes) {
      this.name = name;
      increment(attributes);
    }

    public boolean getLast() {
      return last;
    }

    public void setLast() {
      last = true;
    }

    public void increment(List<String> attributes) {
      if (attributes.contains("EASY")) easy++; else if (
        attributes.contains("MEDIUM")
      ) medium++; else if (attributes.contains("HARD")) hard++; else other++;
    }

    public String getName() {
      return name;
    }

    public Integer getEasy() {
      return easy;
    }

    public Integer getMedium() {
      return medium;
    }

    public Integer getHard() {
      return hard;
    }

    public Integer getOther() {
      return other;
    }
  }
}
