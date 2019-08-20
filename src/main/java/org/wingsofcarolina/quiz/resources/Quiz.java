package org.wingsofcarolina.quiz.resources;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.common.QuizBuildException;
import org.wingsofcarolina.quiz.domain.Attribute;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.Recipe;
import org.wingsofcarolina.quiz.domain.Section;
import org.wingsofcarolina.quiz.domain.Selection;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;

public class Quiz {
	private static final Logger LOG = LoggerFactory.getLogger(Quiz.class);

	public enum QuizType { FAR, SOP_STUDENT, SOP_PILOT, SOP_INSTRUCTOR, C152, C172, PA28, M20J };

	public final static Integer MONTHS_TO_LIVE = 3;
	
	private long quizId;
	private String quizName;
	private QuizType quizType;
	private Category category;
	private List<Question> questions = new ArrayList<Question>();
	@Transient
	private Date createdDate = new Date();

	public Quiz() {}
	
	public Quiz(String request) {
		this.quizId = Persistence.instance().getID("quiz", 1000);
		switch (request) {
			case "far":
				category = Category.FAR;
				quizType = QuizType.FAR;
				quizName = "FAR 61/91";
				break;
			case "sop-student":
				category = Category.SOP;
				quizType = QuizType.SOP_STUDENT;
				quizName = "SOP - Student Pilot";
				break;
			case "sop-pilot":
				category = Category.SOP;
				quizType = QuizType.SOP_PILOT;
				quizName = "SOP - Licensed Pilot"; 
				break;
			case "sop-instructor":
				category = Category.SOP;
				quizType = QuizType.SOP_INSTRUCTOR;
				quizName = "SOP - Instructor";
				break;
			case "c152": category = Category.C152; quizType = QuizType.C152; quizName = "Cessna 152"; break;
			case "c172": category = Category.C172; quizType = QuizType.C172; quizName = "Cessna 172 Skyhawk"; break;
			case "pa28": category = Category.PA28; quizType = QuizType.PA28; quizName = "Piper PA-28 Warrior"; break;
			case "m20j": category = Category.M20J; quizType = QuizType.M20J; quizName = "Mooney M20J"; break;
		}			
	}
	
	public static QuizType getTypeFromName(String name) {
		QuizType type = null; 
		switch (name) {
			case "far":
				type = QuizType.FAR;
				break;
			case "sop-student":
				type = QuizType.SOP_STUDENT;
				break;
			case "sop-pilot":
				type = QuizType.SOP_PILOT;
				break;
			case "sop-instructor":
				type = QuizType.SOP_INSTRUCTOR;
				break;
			case "c152": type = QuizType.C152; break;
			case "c172": type = QuizType.C172; break;
			case "pa28": type = QuizType.PA28; break;
			case "m20j": type = QuizType.M20J; break;
		}
		return type;
	}

	public long getQuizId() {
		return quizId;
	}
	
	public String getQuizName() {
		return quizName;
	}
	
	public Category getCategory() {
		return category;
	}
	
	public List<Question>getQuestions() {
		return questions;
	}
	
	public Date getCreatedDate() {
		return createdDate;
	}

	public Date getSunsetDate() {
	       // convert date to calendar
        Calendar c = Calendar.getInstance();
        c.setTime(createdDate);

        // manipulate date
        c.add(Calendar.MONTH, MONTHS_TO_LIVE);

		return c.getTime();
	}

	public Double pointsPerQuestion() {
		return new Double(100.0 / questions.size());
	}
	
	/**
	 * Based on a quiz recipe, build a quiz
	 * @return
	 * @throws QuizBuildException 
	 */
	public Quiz build() throws QuizBuildException {
		List<Question> pool = new ArrayList<Question>();

		// Pick up the recipe for the desired quiz
		Recipe recipe = Recipe.getRecipeByType(quizType);

		// Iterate over all sections
		for (Section section : recipe.getSections()) {
			// Create a place to deposit all the candidate questions
			List<Question> candidates = new ArrayList<Question>();
			
			// If there are any required questions, pull them into the pool
			if (section.getRequired() != null) {
				for (Long id : section.getRequired()) {
					Question candidate = Question.getByQuestionId(id);
					while (candidate != null && candidate.isSuperceded()) {
						candidate = Question.getByQuestionId(candidate.getSupercededBy());
					}

					if (candidate != null) {
						pool.add(candidate);
						if ( ! candidate.getDeployed()) {
							candidate.setDeployed(true);
							candidate.save();
						}
					} else {
						LOG.error("Required question {} not found. WTF?", id);
					}
				}
			}
			
			// Next iterate over all selections within the section
			for (Selection selection : section.getSelections()) {
				List<String> atts = selection.getAttributes();
				if (atts.contains(Attribute.ANY)) {
					candidates = Question.getSelected(category);
				} else {
					candidates = Question.getSelected(category, atts);
				}
				int candidateCount = candidates.size();
				if (candidateCount < selection.getCount()) {
					throw new QuizBuildException("Not enough candidates to satisfy the Recipe. Had " + candidateCount + " but wanted " + selection.getCount() + ".");
				}
				
				// Select the desired number of questions from the candidates
				int i = 0;
				while (i < selection.getCount()) {
					int pick = 	(int)(Math.random() * candidates.size());
					Question candidate = candidates.get(pick);
					candidates.remove(pick);
					
					// If the question has been supersededed, then find the most
					// current version and use that instead.
					while (candidate != null && candidate.isSuperceded()) {
						candidate = Question.getByQuestionId(candidate.getSupercededBy());
					}
					
					// If the candidate is _not_ already in the pool (which can
					// happen due to conflicts with the REQUIRED list) then we
					// can add it, otherwise we skip it and press on.
					if (notInPool(candidate, pool)) {
						pool.add(candidate);
						i++;
					}
				}
			}
		}
		
		// Pull randomly from the pool, setting sequence number as we go
		int count = pool.size();
		for (int i = 0; i < count; i++) {
			int size = pool.size();
			int pick = (int)(Math.random() * size);
			Question entity = pool.remove(pick);
			entity.setIndex(i + 1);
			questions.add(entity);
			
			// Make note that the question has now been 'deployed'
			if ( ! entity.getDeployed()) {
				entity.setDeployed(true);
				entity.save();
			}
		}
		return this;
	}
	
	/**
	 * Insure that we don't allow duplicates in the pool
	 * 
	 * @param candidate
	 * @param pool
	 * @return
	 */
	public boolean notInPool(Question candidate, List<Question> pool) {
		for (Question selected : pool) {
			if (selected.getQuestionId() == candidate.getQuestionId()) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 */
	public static Quiz quizFromRecord(Record record) {
		Quiz quiz = new Quiz();
		quiz.setQuizId(record.getQuizId());
		quiz.setQuizName(record.getQuizName());
		quiz.setCategory(record.getCategory());
		int i = 1;
		for (Long id : record.getQuestionIds()) {
			Question question = Question.getByQuestionId(id);
			question.setIndex(i++);
			quiz.addQuestion(question);
		}
		return quiz;
	}
	
	protected void addQuestion(Question question) {
		questions.add(question);
	}

	public QuizType getQuizType() {
		return quizType;
	}

	protected void setQuizType(QuizType quizType) {
		this.quizType = quizType;
	}

	public void setQuizId(long quizId) {
		this.quizId = quizId;
	}

	protected void setQuizName(String quizName) {
		this.quizName = quizName;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	/**
	 * Return the record of selected questions
	 */
	public Record getRecord() {
		Record record = new Record(this);
		return record;
	}
}
