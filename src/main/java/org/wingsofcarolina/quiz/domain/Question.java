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
    private Type type;
    private Category category;
    private QuestionDetails details;
	private List<String> attributes;
	private Long questionId;
	private Boolean deployed = false;
	private Boolean deleted = false;
	private long supercededBy = -1;
	private Date createdDate = new Date();

	public Question() {}
	
	public Question(Type type, Category category,  List<String> attributes, QuestionDetails details) {
		this.type = type;
		this.category = category;
		this.attributes = attributes;
		this.details = details;
		this.questionId = Persistence.instance().generateAutoIncrement("question", 1000);
	}
	
	public Question(Type type, Category category, List<String> attributes, String question, String references, List<Answer> answers, String discussion) {
		super();
		this.type = type;
		this.category = category;
		this.attributes = attributes;
		this.details = new QuestionDetails(question, references, answers, discussion);
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
		attributes.add(attribute);
	}

	public boolean hasAttribute(String attribute) {
		for (String att : attributes) {
			if (att.equals(attribute) ) {
				return true;
			}
		}
		return false;
	}

	public Boolean getDeployed() {
		return deployed;
	}

	public void setDeployed(Boolean deployed) {
		this.deployed = deployed;
	}
	
	public Boolean getDeleted() {
		return deleted;
	}

	public void setDeleted(Boolean deleted) {
		this.deleted = deleted;
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
		return details.getQuestion();
	}

	@JsonIgnore
	public String getQuestionAsHtml() {
		return toHtml(details.getQuestion());
	}

	public void setQuestion(String question) {
		details.setQuestion(question);
	}

	public String getReferences() {
		return details.getReference();
	}

	public void setReferences(String references) {
		details.setReference(references);
	}

	public List<Answer> getAnswers() {
		return details.getAnswers();
	}

	public Answer getAnswerAt(int index) {
		if (index-1 < details.getAnswers().size()) {
			return details.getAnswers().get(index-1);
		} else {
			return new EmptyAnswer();
		}
	}
	
	public void setAnswers(List<Answer> answers) {
		details.setAnswers(answers);
	}

	public String getDiscussion() {
		return details.getDiscussion();
	}

	public void setDiscussion(String discussion) {
		details.setDiscussion(discussion);
	}
	
	private String toHtml(String s) {
	    StringBuilder builder = new StringBuilder();
	    boolean previousWasASpace = false;
	    for( char c : s.toCharArray() ) {
	        if( c == ' ' ) {
	            if( previousWasASpace ) {
	                builder.append("&nbsp;");
	                previousWasASpace = false;
	                continue;
	            }
	            previousWasASpace = true;
	        } else {
	            previousWasASpace = false;
	        }
	        switch(c) {
	            case '<': builder.append("&lt;"); break;
	            case '>': builder.append("&gt;"); break;
	            case '&': builder.append("&amp;"); break;
	            case '"': builder.append("&quot;"); break;
	            case '\n': builder.append("<br>"); break;
	            // We need Tab support here, because we print StackTraces as HTML
	            case '\t': builder.append("&nbsp; &nbsp; &nbsp;"); break;  
	            default:
	                if( c < 128 ) {
	                    builder.append(c);
	                } else {
	                    builder.append("&#").append((int)c).append(";");
	                }    
	        }
	    }
	    return builder.toString();
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
		return "Question [id=" + id + ", index=" + index + ", details=" + details + ", questionId=" + questionId
				+ ", deployed=" + deployed + ", supercededBy=" + supercededBy + ", createdDate=" + createdDate + "]";
	}
}
