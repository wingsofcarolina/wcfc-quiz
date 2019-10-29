package org.wingsofcarolina.quiz.scripting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.resources.Quiz;
import org.wingsofcarolina.quiz.resources.QuizContext;

import groovy.lang.Script;

public abstract class QuizDSL extends Script {
	private static final Logger LOG = LoggerFactory.getLogger(QuizDSL.class);

    private QuizContext context;
    private Integer sectionCount;
	private Set<Question> section = null;
    private Map<String, List<Long>> exclusives = new HashMap<String, List<Long>>();

    public QuizContext getContext() {
        return context;
    }

    public void setContext(QuizContext context) {
        this.context = context;
    }

    // Implement classic Groovy method/property missing behavior
    public Object propertyMissing(String name) {
        return "Missing property " + name;
    }
    public Object methodMissing(String name, Object args) {
        List<Object> argsList = Arrays.asList((Object[]) args);
        LOG.debug("methodMissing called for ---> {}", name);
        return "methodMissing called with name '" + name + "' and args = " + argsList;
    }
    
    // Methods supporting the Prefect DSL functions
    public void instructions(String value) {
    	context.setVariable("instructions", value);
    }
    
    // Override the category
    public void category(String name) {
    	context.getQuiz().setCategory(Category.valueOf(name.toUpperCase()));
    }
  
    // Trim a string to the first newline, if one exists
    public String trim(String line) {
    	String result = line;
    	int pos = line.indexOf('\n');
    	if (pos != -1) {
    		result = line.substring(0, pos);
    	}
    	return result;
    }
    
    // Start a "section" of the quiz
    public void startSection(Integer sectionCount, String sectionName) {
    	LOG.info("Starting new section : {}", sectionName);
    	this.sectionCount = sectionCount;
    	section = new HashSet<Question>();
    }
    
    // Apply the collection set of questions to the quiz
    public void applySection(String name) {
    	LOG.info("Applying collected section : {}", name);
    	List<Question> questions = new ArrayList<Question>();
    	questions.addAll(section);
    	context.getQuiz().addAll(questions);
    }
    
    // Add questions to the selected set of questions in the quiz
    // NOTE: The items are pulled out of the pool in a random order
    // to increase the entropy of each generated quiz.
    public void select(List<Question> pool) {
    	if (section != null) {
    		List<Question> result = randomize(pool);
			for (Question entity : result) {
				// Shortcut out if we've collected enough questions
				if (sectionCount == 0) break;
				
				// Resolve the actual question (i.e. follow the superseded chain)
				entity = resolve(entity);
				
				// Add the question to the section if it has not been used before
				if ( ! alreadySelected(entity)) {
					// Add it and keep track of the count
					section.add(entity);
					sectionCount--;
					
					// Make note that the question has now been 'deployed'
					if (context.getConfiguration().getMode().contentEquals("PROD")) {
						if ( ! entity.getDeployed()) {
							entity.setDeployed(true);
							entity.save();
						}
					}
				}
			}
    	} else {
    		LOG.info("Attempted to add questions outside of a section");
    	}
    }
    
    // Add a specified number of questions to the selected set of questions in the quiz
    // NOTE: The items are pulled out of the pool in a random order
    // to increase the entropy of each generated quiz.
    public void select(Integer count, List<Question> pool) {
    	if (section != null) {
	    	if (count <= pool.size()) {
	    		List<Question> result = randomize(pool);
				for (Question entity : result) {
					// Shortcut out if we've collected enough questions
					if (sectionCount == 0) break;
					
					// Add the question to the section if it has not been used before
					if ( ! alreadySelected(entity)) {
						// Add it and keep track of the count
						section.add(entity);
						sectionCount--;
						
						// Make note that the question has now been 'deployed'
						if (context.getConfiguration().getMode().contentEquals("PROD")) {
							if ( ! entity.getDeployed()) {
								entity.setDeployed(true);
								entity.save();
							}
						}
					}
				}
	    	} else {
	    		LOG.info("Selection attempted with an undersize pool, wanted {} and only had {}", count, pool.size());
	    	}
    	} else {
    		LOG.info("Attempted to add questions outside of a section");
    	}
    }

    // Follow the superseded chain to the current version of the question
    // being processed. 
    private Question resolve(Question question) {
    	Question result = question;
    	
    	while (question.isSuperseded()) {
    		long nextId = question.getSupersededBy();
    		LOG.info("Question {} superseded by {}", question.getQuestionId(), nextId);
    		result = Question.getByQuestionId(nextId);
    	}
    	
    	return result;
    }
    
    private List<Question> randomize(List<Question> pool) {
    	List<Question> result = new ArrayList<Question>();
    	
    	if (pool.size() > 0) {
			do {
				int size = pool.size();
				if (size > 0) {
					int pick = (int)(Math.random() * size);
					result.add(pool.remove(pick));
				} else {
					LOG.info("Why did the pool get to zero???");
				}
			} while (pool.size() > 0);
    	}
    	
    	return result;
    }
    
    private boolean alreadySelected(Question candidate) {
    	if (context.getQuiz().hasQuestion(candidate)) {
    		return true;
    	}
    	long id = candidate.getQuestionId();
		for (Question question : section) {
			if (question.getQuestionId() == id) {
				LOG.info("Found a conflict, rejecting {}", id);
				return true;
			}
		}

    	return false;
    }
    
    // Create a mutually exclusive pool
    public void exclusive(String name, List<Integer>questionIds) {
    	if (name != null && questionIds != null && questionIds.size() > 1) {
    		List<Long> group = new ArrayList<Long>();
	      	for (Integer id : questionIds) {
	      		Long qid = new Long(id);
	    		Question question = resolve(Question.getByQuestionId(qid));
	    		if (question != null) {
	    			group.add(qid);
	    		} else {
	    			LOG.info("Requested question {} not found", id);
	    		}
	    	}
	      	exclusives.put(name, group);
    	}
    }
    
    // Filter the list returning only those questions which contain the 
    // indicated attribute.
    public List<Question> filter(String attribute, List<Question> pool) {
    	List<Question> result = new ArrayList<Question>();
    	for (Question question: pool) {
    		if (question.getAttributes().contains(attribute)) {
    			result.add(question);
    		}
    	}
    	return result;
    }
    
    // Get a list of questions by question IDs
    public List<Question> getQuestions(List<Integer> questionIds) {
    	Set<Question> questions = new HashSet<Question>();
    	for (Integer id : questionIds) {
    		Question question = Question.getByQuestionId(new Long(id));
    		if (question != null) {
    			if (question.getCategory().equals(context.getQuiz().getCategory())) {
    				questions.add(question);
    			} else {
        			LOG.info("Requested question {} does not match quiz category {}", id, context.getQuiz().getCategory());
    			}
    		} else {
    			LOG.info("Requested question {} not found", id);
    		}
    	}
    	return new ArrayList<Question>(questions);
    }
    
    // Get a list of questions by category
    public List<Question> getQuestions() {
    	Category category = context.getQuiz().getCategory();
    	List<Question> result = Question.getSelected(category);
		return result;
    }
    
    // Get a list of questions by category, filtered by attributes
    public List<Question> getQuestionsWithAny(List<String> atts) {
    	Category category = context.getQuiz().getCategory();
    	List<Question> result = new ArrayList<Question>();
    	for (Question question : Question.getSelected(category)) {
    		for (String attribute : question.getAttributes()) {
    			if (atts.contains(attribute)) {
    				result.add(question);
    				break;
    			}
    		}
    	}
		return result;
    }
}
