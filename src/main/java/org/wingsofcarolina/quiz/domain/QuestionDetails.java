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
	private String difficulty;
	private List<Answer> answers;
	
	public QuestionDetails() {}
	
	public QuestionDetails(String question, String reference,
			List<Answer> answers, String discussion) {
		this.question = question;
		this.reference = reference;
		this.answers = answers;
		this.discussion = discussion;
	}

	public QuestionDetails(String question, String discussion, String references,
			String answer1, String answer2, String answer3, String answer4,
			String answer5, Boolean correct1, Boolean correct2, Boolean correct3, Boolean correct4, Boolean correct5) {

		LOG.info("Question   --> {}", question);
		LOG.info("Discussion --> {}", discussion);
		LOG.info("References --> {}", references);

		this.question = question;
		this.discussion = discussion;
		this.reference = references;
		
		// Remove any nulls
		correct1 = (correct1 == null) ? new Boolean(false) : correct1;
		correct2 = (correct2 == null) ? new Boolean(false) : correct2;
		correct3 = (correct3 == null) ? new Boolean(false) : correct3;
		correct4 = (correct4 == null) ? new Boolean(false) : correct4;
		correct5 = (correct5 == null) ? new Boolean(false) : correct5;

		LOG.info("Answer1    --> {}, {}", answer1, correct1);
		LOG.info("Answer2    --> {}, {}", answer2, correct2);
		LOG.info("Answer3    --> {}, {}", answer3, correct3);
		LOG.info("Answer4    --> {}, {}", answer4, correct4);
		LOG.info("Answer5    --> {}, {}", answer5, correct5);
		
		List<Answer> answers = new ArrayList<Answer>();
		if ( ! answer1.isEmpty()) answers.add(new Answer(answer1, correct1));
		if ( ! answer2.isEmpty()) answers.add(new Answer(answer2, correct2));
		if ( ! answer3.isEmpty()) answers.add(new Answer(answer3, correct3));
		if ( ! answer4.isEmpty()) answers.add(new Answer(answer4, correct4));
		if ( ! answer5.isEmpty()) answers.add(new Answer(answer5, correct5));
		
		// Reset the indexes to the current order
		int i = 1;
		for (Answer a : answers) {
			a.setIndex(i++);
		}
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

	public String getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	public void setAnswers(List<Answer> answers) {
		this.answers = answers;
	}
}
