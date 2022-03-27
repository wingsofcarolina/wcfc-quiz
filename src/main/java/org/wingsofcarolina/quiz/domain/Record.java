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
	private Date createdDate = new Date();
    private List<Long> questionIds = new ArrayList<Long>();

	public Record() {}
	
	public Record(Quiz quiz) {
		this.quizId = quiz.getQuizId();
		this.quizName = quiz.getQuizName();
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

	public List<Long> getQuestionIds() {
		return questionIds;
	}

	public void add(Long index) {
		questionIds.add(index);
	}
	
	@Override
	public String toString() {
		return "Record [quizId=" + quizId + ", quizName=" + quizName + ", createdDate="
				+ createdDate + ", questionIds=" + questionIds + "]";
	}

	/*
	 * Database Management Functionality
	 */
	public static List<Record> getAllRecords() {
		RecordDAO RecordDao = (RecordDAO) Persistence.instance().get(Record.class);
		return RecordDao.getAllRecords();
	}
	
	public static Record getByQuizId(Long id) {
		RecordDAO recordDAO = (RecordDAO) Persistence.instance().get(Record.class);
		return recordDAO.getByQuizId(id);
	}

	public static Set<Long> getActiveIds() {
		RecordDAO recordDAO = (RecordDAO) Persistence.instance().get(Record.class);
		return recordDAO.getDeployedIds();
	}
	
	public static List<Record> getEarlierThan(Date sunset) {
		RecordDAO recordDAO = (RecordDAO) Persistence.instance().get(Record.class);
		return recordDAO.getEarlierThan(sunset);
	}

	public static Boolean isQuestionIdDeployed(Long questionId) {
		RecordDAO recordDAO = (RecordDAO) Persistence.instance().get(Record.class);
		return recordDAO.isQuestionIdDeployed(questionId);
	}
	
	@SuppressWarnings("unchecked")
	public void save() {
		Persistence.instance().get(Record.class).save(this);
	}
	
	@SuppressWarnings("unchecked")
	public void delete() {
		Persistence.instance().get(Record.class).delete(this);
	}
}
