package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.dao.RecordDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.resources.Quiz;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Record {
    @Id
	@JsonIgnore
    private ObjectId id;
    
	private Long quizId;
	private String quizName;
	private Category category;
	private Date createdDate = new Date();
    private List<Long> questionIds = new ArrayList<Long>();

	public Record() {}
	
	public Record(Quiz quiz) {
		this.quizId = quiz.getQuizId();
		this.quizName = quiz.getQuizName();
		this.category = quiz.getCategory();
		for (Question question : quiz.getQuestions()) {
			questionIds.add(question.getQuestionId());
		}
	}

	public long getQuizId() {
		return quizId;
	}

	public void setQuizId(long quizId) {
		this.quizId = quizId;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public String getQuizName() {
		return quizName;
	}

	public void setQuizName(String quizName) {
		this.quizName = quizName;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public List<Long> getQuestionIds() {
		return questionIds;
	}

	public void add(Long index) {
		questionIds.add(index);
	}
	
	/*
	 * Database Management Functionality
	 */
	public static Record getByQuizId(Long id) {
		RecordDAO recordDAO = (RecordDAO) Persistence.instance().get(Record.class);
		return recordDAO.getByQuizId(id);
	}

	public static Set<Long> getActiveIds() {
		RecordDAO recordDAO = (RecordDAO) Persistence.instance().get(Record.class);
		return recordDAO.getDeployedIds();
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
