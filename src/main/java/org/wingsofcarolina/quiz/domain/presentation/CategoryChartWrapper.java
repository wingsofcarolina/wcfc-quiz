package org.wingsofcarolina.quiz.domain.presentation;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.User;

public class CategoryChartWrapper extends QuestionListWrapper {

	private Category category;
	private CategoryStats stats;

	public CategoryChartWrapper(User user, List<Question> questions, Category category) {
		super(user, questions);
		this.category = category;
		this.stats = new CategoryStats(questions);
		
		int count = 0;
		int last = stats.attributes.size() - 1;
		for (AttributeStats stat : stats.attributes.values()) {
			if (count == last) {
				stat.setLast();
			}
			count++;
		}
	}

	public Category getCategory() {
		return category;
	}

	public CategoryStats getStats() {
		return stats;
	}

	public class CategoryStats {
		Map<String, AttributeStats> attributes = new TreeMap<String, AttributeStats>();
		
		public CategoryStats(List<Question> questions) {
			for (Question question : questions) {
				for (String attribute : question.getAttributes()) {
					if (attribute != null) {
						switch (attribute) {
						case "EASY" :
						case "MEDIUM" :
						case "HARD" :
							break;
						default :
							if (attributes.containsKey(attribute)) {
								attributes.get(attribute).increment(question.getAttributes());
							} else {
								attributes.put(attribute, new AttributeStats(attribute, question.getAttributes()));
							}
						}
					}
				}
			}
		}

		public Collection<AttributeStats> getAttributes() {
			return attributes.values();
		}
	}
	
	public class AttributeStats {
		private String name;
		private Integer easy = 0;
		private Integer medium = 0;
		private Integer hard = 0;
		private Integer other = 0;
		private boolean last = false;

		public AttributeStats(String name, List<String> attributes) {
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
			if (attributes.contains("EASY"))
				easy++;
			else if (attributes.contains("MEDIUM"))
				medium++;
			else if (attributes.contains("HARD"))
				hard++;
			else
				other++;
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
