package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionDetails {
	private static final Logger LOG = LoggerFactory.getLogger(QuestionDetails.class);

	private String question;
	private String discussion;
	private String reference;
    private String attachment = "NONE";
	private List<Answer> answers;
	
	public QuestionDetails() {}
	
	public QuestionDetails(String question, String reference, List<Answer> answers, String discussion, String attachment) {
		this.question = question;
		this.reference = reference;
		this.answers = answers;
		this.discussion = discussion;
		this.attachment = attachment;
	}

	public QuestionDetails(String question, String discussion, String references, List<String> answerText, Integer correct, String attachment) {

		LOG.info("Question   --> {}", question);
		LOG.info("Discussion --> {}", discussion);
		LOG.info("References --> {}", references);
		LOG.info("Attachment --> {}", attachment);

		this.question = question;
		this.discussion = discussion;
		this.reference = references;
		this.attachment = attachment;

		int index = 0;
		answers = new ArrayList<Answer>();
		for (String answer : answerText) {
			if ( ! answer.isEmpty() ) {
				if (correct == null) {
					LOG.info("Answer{}    --> {}, fill in the blank", index, answer);
					answers.add(new Answer(answer));
				} else {
					LOG.info("Answer{}    --> {}, {}", index, answer, correct == index ? true : false);
					answers.add(new Answer(answer, correct == index ? true : false));
				}
				index++;
			}
		}
	}

	public QuestionDetails(String question, String discussion, String references, List<String> answers, String attachment) {
		this(question, discussion, references, answers, null, attachment);
	}

	public int compareTo(QuestionDetails details) {
		if ( ! question.equals(details.getQuestion())) return -1;
		if ( ! discussion.equals(details.getDiscussion())) return -1;
		if ( ! reference.equals(details.getReference())) return -1;
		if ( ! attachment.equals(details.getAttachment())) return -1;

		List<Answer> newAnswers = details.getAnswers();
		if (answers != null && newAnswers != null) {
			if (answers.size() != newAnswers.size()) return -1;
			for (int i = 0; i < answers.size(); i++) {
				if (answers.get(i).compareTo(newAnswers.get(i)) != 0) return -1;
			}
		} else {
			return -1;
		}
		
		return 0;
	}
	
	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getDiscussion() {
		return discussion;
	}

	public void setDiscussion(String discussion) {
		this.discussion = discussion;
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}
	
	public String getAttachment() {
		return attachment;
	}

	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}
}
