package org.wingsofcarolina.quiz.scripting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.wingsofcarolina.quiz.domain.Attribute;
import org.wingsofcarolina.quiz.domain.Question;

public class Pool {
	private List<Question> pool;
	
	public Pool() {
		this.pool = new ArrayList<Question>();
	}
	
	public Pool(List<Question> questions) {
		this.pool = questions;
	}

	public void setQuestionList(List<Question> pool) {
		this.pool = pool;
	}

	public List<Question> getQuestionList() {
		return pool;
	}

	public Integer size() {
		return pool.size();
	}
	
    public Pool randomize() {
    	List<Question> result = new ArrayList<Question>();
    	
    	if (pool.size() > 0) {
			do {
				int size = pool.size();
				if (size > 0) {
					int pick = (int)(Math.random() * size);
					result.add(pool.remove(pick));
				}
			} while (pool.size() > 0);
    	}
    	
    	this.pool = result;
    	return this;
    }
	
	@Override
	public String toString() {
		return "Pool [pool=" + pool + "]";
	}

	public Pool add(Question question) {
		pool.add(question);
		return this;
	}
	
	public Pool addAll(List<Question> allQuestions) {
		pool.addAll(allQuestions);
		return this;
	}
	
    // Get a list of questions by category, filtered by attributes
    public Pool questionsWithAny(List<String> atts) {
		if (atts.contains(Attribute.ANY)) {
			pool = Question.getAllQuestions();
		} else {
	    	List<Question> result = new ArrayList<Question>();
	    	for (Question question : Question.getAllQuestions()) {
	    		if (question.containsAny(atts)) {
	    			result.add(question);
	    		}
	    	}
	    	pool.addAll(result);
		}
		return this;
    }
    
    // Filter the list returning only those questions which contain the 
    // indicated attributes.
    public Pool includeOnly(List<String> attributes) {
    	List<Question> result = new ArrayList<Question>();
    	for (Question question: pool) {
    		if (question.containsAny(attributes)) {
    			result.add(question);
    		}
    	}
    	this.pool = result;
    	return this;
    }
    
    
    // Filter the list removing all those questions which contain the 
    // indicated attributes.
    public Pool excludeAll(List<String> attributes) {
    	Iterator<Question> it = pool.iterator();
    	while (it.hasNext()) {
    		Question question = it.next();
    		if (question.containsAny(attributes)) {
    			it.remove();
    		}
    	}
    	return this;
    }
}
