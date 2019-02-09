package org.wingsofcarolina.quiz.domain;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.domain.dao.QuestionDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Question {
    @Id
	@JsonIgnore
    private ObjectId id;
	private long questionId;
	private Type type;
	private Category category;
	private SubCategory subCategory;
	private Boolean deprecated = false;
	private Date createdDate = new Date();
	private String question;
	private String references;
	private List<Answer> answers;
	private String discussion;
	
	public Question() {}
	
	public Question(Type type, Category category, SubCategory subCategory, String question, String references, List<Answer> answers, String discussion) {
		super();
		this.type = type;
		this.category = category;
		this.subCategory = subCategory;
		this.question = question;
		this.references = references;
		this.answers = answers;
		this.discussion = discussion;
		this.questionId = Persistence.instance().generateAutoIncrement("question", 1000);
	}

	public boolean isBlank() {
		return type == Type.BLANK;
	}
	
	public long getQuestionId() {
		return questionId;
	}

	public void setQuestionId(Integer questionId) {
		this.questionId = questionId;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Category getCategory() {
		return category;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public SubCategory getSubCategory() {
		return subCategory;
	}

	public void setSubCategory(SubCategory subCategory) {
		this.subCategory = subCategory;
	}

	public Boolean getDeprecated() {
		return deprecated;
	}

	public void setDeprecated(Boolean deprecated) {
		this.deprecated = deprecated;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getReferences() {
		return references;
	}

	public void setReferences(String references) {
		this.references = references;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public String getDiscussion() {
		return discussion;
	}

	public void setDiscussion(String discussion) {
		this.discussion = discussion;
	}
	
	/*
	 * Database Management Functionality
	 */
	public static List<Question> getAllQuestions() {
		QuestionDAO userDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return userDao.getAllQuestions();
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
