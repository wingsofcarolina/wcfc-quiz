package org.wingsofcarolina.quiz.domain;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;
import org.wingsofcarolina.quiz.domain.dao.QuestionDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Question {
    @Id
	@JsonIgnore
    private ObjectId id;
    @JsonIgnore
    @Transient
    private Integer index;
	private Long questionId;
	private Type type;
	private Category category;
	private List<String> attributes;
	private Boolean deployed = false;
	private long supercededBy = -1;
	private Date createdDate = new Date();
	private String question;
	private String references;
	private List<Answer> answers;
	private String discussion;
	
	public Question() {}
	
	public Question(Type type, Category category, List<String> attribute, String question, String references, List<Answer> answers, String discussion) {
		super();
		this.type = type;
		this.category = category;
		this.attributes = attribute;
		this.question = question;
		this.references = references;
		this.answers = answers;
		this.discussion = discussion;
		this.questionId = Persistence.instance().generateAutoIncrement("question", 1000);
	}

	@JsonIgnore
	public boolean isFillInTheBlank() {
		return type == Type.BLANK;
	}
	
	@JsonIgnore
	public boolean isMultipleChoice() {
		return type != Type.BLANK;
	}
	
	public long getQuestionId() {
		return questionId;
	}

	public void setQuestionId(long questionId) {
		this.questionId = questionId;
	}

	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
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

	public List<String> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<String> attributes) {
		this.attributes = attributes;
	}
	
	public void addAttribute(String attribute) {
		this.attributes.add(attribute);
	}

	public Boolean getDeployed() {
		return deployed;
	}

	public void setDeployed(Boolean deployed) {
		this.deployed = deployed;
	}
	
	public long getSupercededBy() {
		return supercededBy;
	}

	public void setSupercededBy(long supercededBy) {
		this.supercededBy = supercededBy;
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

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
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
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getAllQuestions();
	}


	public static List<Question> getQuestionsLimited(int skip, int count) {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getQuestionsLimited(skip, count);
	}
	
	public static Collection<? extends Question> getSelected(Category category) {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getSelected(category);
	}

	public static List<Question> getSelected(Category category, List<String> attributes) {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getSelected(category, attributes);
	}

	public static Question getByQuestionId(Long id) {
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

	@Override
	public String toString() {
		return "Question [questionId=" + questionId + ", type=" + type + ", category=" + category + ", attributes="
				+ attributes + "]";
	}

}
