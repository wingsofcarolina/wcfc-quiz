package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.dao.QuestionDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.domain.quiz.Quiz.QuizType;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Record {
    @Id
	@JsonIgnore
    private ObjectId id;
    
	private Long quizId;
	private String quizName;
	private QuizType quizType;
	private Category category;
	private Date createdDate = new Date();
    private List<Long> questionIds = new ArrayList<Long>();

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

	public QuizType getQuizType() {
		return quizType;
	}

	public void setQuizType(QuizType quizType) {
		this.quizType = quizType;
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
	public static Question getByQuizId(Long id) {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getByQuestionId(id);
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
