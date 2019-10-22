package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionDetails {
	private static final Logger LOG = LoggerFactory.getLogger(QuestionDetails.class);

	private static final int MAX_ANSWERS = 5;
	
	private String question;
	private String discussion;
	private String reference;
	private List<Answer> answers;
	
	public QuestionDetails() {}
	
	public QuestionDetails(String question, String reference, List<Answer> answers, String discussion) {
		this.question = question;
		this.reference = reference;
		this.answers = answers;
		this.discussion = discussion;
	}

	public QuestionDetails(String question, String discussion, String references, List<String> answerText, Integer correct) {

		LOG.info("Question   --> {}", question);
		LOG.info("Discussion --> {}", discussion);
		LOG.info("References --> {}", references);

		this.question = question;
		this.discussion = discussion;
		this.reference = references;

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

	public QuestionDetails(String question, String discussion, String references, List<String> answers) {
		this(question, discussion, references, answers, null);
	}

	public int compareTo(QuestionDetails details) {
		if ( ! question.equals(details.getQuestion())) return -1;
		if ( ! discussion.equals(details.getDiscussion())) return -1;
		if ( ! reference.equals(details.getReference())) return -1;

		List<Answer> newAnswers = details.getAnswers();
		if (answers.size() != newAnswers.size()) return -1;
		
		for (int i = 0; i < answers.size(); i++) {
			if (answers.get(i).compareTo(newAnswers.get(i)) != 0) return -1;
		}
		
		return 0;
	}
	
	/**
	 * This method updates each detail field in the question and detects
	 * whether any change has occurred. The change value is used to gate
	 * updating the question in the database so we don't make gratuitous
	 * changes.
	 * 
	 * @param original
	 * @return
	 */
//	public boolean update(Question original) {
//		boolean changed = false;
//		
//		if ( ! original.getQuestion().contentEquals(question)) {
//			original.setQuestion(question);
//			changed = true;
//		}
//		if ( ! original.getDiscussion().contentEquals(discussion)) {
//			original.setDiscussion(discussion);
//			changed = true;
//		}
//		if ( ! original.getReferences().contentEquals(reference)) {
//			original.setReferences(reference);
//			changed = true;
//		}
//		for (int i = 0; i < MAX_ANSWERS; i++) {
//			Answer newAnswer = answers.get(i);
//			Answer oldAnswer = original.getAnswers().get(i);
//			if ( ! newAnswer.getAnswer().contentEquals(oldAnswer.getAnswer())) {
//				oldAnswer.setAnswer(newAnswer.getAnswer());
//				changed = true;
//			}
//			if (newAnswer.isCorrect() != oldAnswer.isCorrect() ) {
//				oldAnswer.setCorrect(newAnswer.isCorrect());
//				changed = true;
//			}
//		}
//		return changed;
//	}
	
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
}
