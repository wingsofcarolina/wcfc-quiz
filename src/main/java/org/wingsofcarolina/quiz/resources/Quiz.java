package org.wingsofcarolina.quiz.resources;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.common.QuizBuildException;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.Recipe;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.scripting.Execute;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Quiz {
	private static final Logger LOG = LoggerFactory.getLogger(Quiz.class);

	public final static Integer MONTHS_TO_LIVE = 3;
	
	private long quizId;
	private String quizName;
	private Category category;
	private List<Question> questions = new ArrayList<Question>();
	private Date createdDate = new Date();
	private Recipe recipe = null;
	private QuizContext context = null;

	Execute execute;

	public Quiz() {}
	
	public Quiz(Recipe recipe) {
		this.quizId = Persistence.instance().getID("quiz", 1000);
		this.recipe = recipe;
		this.quizName = recipe.getName();
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

	public QuizContext getContext() {
		return context;
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
		return Math.floor(new Double(100.0 / questions.size()));
	}
	
	/**
	 * Based on a quiz recipe, build a quiz
	 * @return
	 * @throws QuizBuildException 
	 */
	public Quiz build(QuizContext context) throws QuizBuildException {
		this.context = context;
		execute = new Execute(context);

		if (recipe.getScript() != null) {
			Map<String, String> args = null;
			String result = execute.run(recipe.getScript(), args );
			LOG.info("Script returned : {}", result);
		}
		
		// Add index/sequence numbers to the questions
    	int index = 0;
    	for (Question question : questions) {
    		question.setIndex(index++);
    	}
    	
		return this;
	}
	
	public void addQuestion(Question question) {
		questions.add(question);
	}


	public void addAll(List<Question> questions) {
		for (Question question : questions) {
			addQuestion(question);
		}
	}
	
	@JsonIgnore
	public boolean hasQuestion(Question candidate) {
		if (candidate != null) {
			long id = candidate.getQuestionId();
			for (Question question : questions) {
				if (question.getQuestionId() == id) {
					return true;
				}
			}
		}
		return false;
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
