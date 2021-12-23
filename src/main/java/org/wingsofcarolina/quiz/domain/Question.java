package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.bson.types.ObjectId;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Transient;
import org.wingsofcarolina.quiz.domain.dao.QuestionDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.domain.presentation.CmRenderer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itextpdf.layout.element.Paragraph;

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
	private List<Long> excludes;
	private Long questionId;
	private Boolean deployed = false;
	private Boolean deleted = false;
	private Boolean quarantined = false;
	private long supersededBy = -1;
	private Date createdDate = new Date();
	@JsonIgnore
	@Transient
	private Parser parser;
	@JsonIgnore
	@Transient
	private HtmlRenderer renderer;
	
	public static String ID_KEY = "question";

	public Question() {
		details = new QuestionDetails();
	}
	
	public Question(Type type, Category category,  List<String> attributes, QuestionDetails details) {
		this.type = type;
		this.category = category;
		this.attributes = attributes;
		this.details = details;
		this.questionId = Persistence.instance().getID(ID_KEY, 1000);
	}
	
	public Question(Type type, Category category, List<String> attributes, String question, String references, List<Answer> answers, String discussion, String attachment) {
		super();
		this.type = type;
		this.category = category;
		this.attributes = attributes;
		this.details = new QuestionDetails(question, references, answers, discussion, attachment);
		this.questionId = Persistence.instance().getID(ID_KEY, 1000);
	}

	@JsonIgnore
	public boolean isFillInTheBlank() {
		return type == Type.BLANK;
	}
	
	@JsonIgnore
	public boolean isMultipleChoice() {
		return type != Type.BLANK;
	}
	
	@JsonIgnore
	public boolean isSuperseded() {
		return supersededBy != -1;
	}

	public long getQuestionId() {
		return questionId;
	}

	public void setQuestionId(long questionId) {
		this.questionId = questionId;
	}
	
	public List<Long> getExcludes() {
		return excludes;
	}

	public void setExclusions(List<Integer> exclusions) {
		// Always replace the existing list
		excludes = new ArrayList<Long>();
		
		// Ensure that we don't add ourselves to the list
		for (Integer id : exclusions) {
			Long qid = Long.valueOf(id);
			if ( ! qid.equals(questionId)) {
				excludes.add(qid);
			}
		}
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
		if (attribute == null) return;
		for (String a : attributes) {
			if (a != null && a.equals(attribute)) {
				return;
			}
		}
		attributes.add(attribute);
	}
	
	public void removeAttribute(String attribute) {
		Iterator<String> it = attributes.iterator();
		while (it.hasNext()) {
			String value = it.next();
			if (attribute.equals(value)) {
				it.remove();
			}
		}
	}
	
	public boolean hasAttribute(String attribute) {
		if (attribute != null) {
			for (String att : attributes) {
				if (att != null && att.equals(attribute) ) {
					return true;
				}
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

	public Boolean getQuarantined() {
		return quarantined;
	}

	public boolean isQuarantined() {
		return quarantined;
	}

	public void setQuarantined(Boolean quarantined) {
		this.quarantined = quarantined;
	}

	public long getSupersededBy() {
		return supersededBy;
	}
	
	public void setSupersededBy(long supersededBy) {
		this.supersededBy = supersededBy;
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
		return CmRenderer.renderAsHtml(details.getQuestion());
	}

	@JsonIgnore
	public Paragraph getQuestionAsIText() {
		return CmRenderer.renderToParagraph(details.getQuestion());
	}

	public void setQuestion(String question) {
		details.setQuestion(question);
	}

	public String getReferences() {
		return details.getReference();
	}

	@JsonIgnore
	public String getReferencesAsHtml() {
		return CmRenderer.renderAsHtml(details.getReference());
	}
	
	public void setReferences(String references) {
		details.setReference(references);
	}

	public List<Answer> getAnswers() {
		return details.getAnswers();
	}

	public Answer getAnswerAt(int index) {
		List<Answer> answers = details.getAnswers();
		if (answers != null && index-1 < answers.size()) {
			return answers.get(index-1);
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

	@JsonIgnore
	public String getDiscussionAsHtml() {
		return CmRenderer.renderAsHtml(details.getDiscussion());
	}

	public void setDiscussion(String discussion) {
		details.setDiscussion(discussion);
	}
	
	@JsonIgnore
	public QuestionDetails getDetails() {
		return details;
	}
	
	@JsonIgnore
	public void setDetails(QuestionDetails details) {
		this.details = details;
	}
	
	@JsonIgnore
	public Object getAttachment() {
		return details.getAttachment();
	}
	
	/*
	 * Database Management Functionality
	 */
	public static void drop() {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		questionDao.drop();
	}
	
	public static List<Question> getAllQuestions() {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getAllQuestions();
	}

	public static List<Question> getByAttribute(String attribute) {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getSelectedWith(attribute);
	}
	
	public static List<Question> getQuestionsLimited(int skip, int count) {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getQuestionsLimited(skip, count);
	}

	public static Question getByQuestionId(Long id) {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getByQuestionId(id);
	}

	public static List<Question> getAllQuarantined() {
		QuestionDAO questionDao = (QuestionDAO) Persistence.instance().get(Question.class);
		return questionDao.getAllQuarantined();
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
		return "Question [questionId=" + questionId + ", index=" + index + ", details=" + details +
				", deployed=" + deployed + ", supersededBy=" + supersededBy + ", createdDate=" + createdDate + "]";
	}

	public boolean containsAny(List<String> atts) {
		for (String attribute : attributes) {
			if (atts.contains(attribute)) {
				return true;
			}
		}
		return false;
	}
}
