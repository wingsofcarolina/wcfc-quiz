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
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Section;
import org.wingsofcarolina.quiz.resources.QuizContext;

import groovy.lang.Script;

public abstract class QuizDSL extends Script {
	private static final Logger LOG = LoggerFactory.getLogger(QuizDSL.class);

    private QuizContext context;
	private Section section = null;
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
        LOG.info("methodMissing called for ---> {}", name);
        if (context.getTestRun()) {
        	System.out.println("<br>ERROR : methodMissing called for : " + name);
        }
        return "methodMissing called with name '" + name + "' and args = " + argsList;
    }
    
    ///////////////////////////////////////////////
    // Methods supporting the Quiz DSL functions
    ///////////////////////////////////////////////
    public void instructions(String value) {
    	context.setVariable("instructions", value);
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
    public void start(Integer questionCount, String sectionName) {
    	if (context.getTestRun()) {
        	System.out.println("<br>### Starting new section : " + sectionName);
    	} else {
        	LOG.info("Starting new section : {}", sectionName);
    	}
    	this.section = new Section(questionCount, sectionName);
    }
    
    // Apply the collection set of questions to the quiz
    public void end(String name) {
    	if (context.getTestRun()) {
        	System.out.println("<br>### Applying collected section : " + name);
    	} else {
        	LOG.info("Applying collected section : {}", name);
    	}
    	if (section != null) {
	    	List<Question> questions = new ArrayList<Question>();
	    	questions.addAll(section.getQuestions());
	    	context.getQuiz().addAll(questions);
    	} else {
    		if (context.getTestRun()) {
            	System.out.println("<br>ERROR : Attempted to add an empty/null section to the quiz. Why?");
    		} else {
        		LOG.info("Attempted to add an empty/null section to the quiz. Why?");
    		}
    	}
    }
    
    // Add questions to the selected set of questions in the quiz
    // NOTE: The items are pulled out of the Pool pool in a random order
    // to increase the entropy of each generated quiz.
    public void select(Pool pool) {
    	if (section != null) {
    		List<Question> result = pool.randomize().getQuestionList();
			for (Question entity : result) {
				// Shortcut out if we've collected enough questions
				if (section.isFull()) break;
				
				// Resolve the actual question (i.e. follow the superseded chain)
				entity = resolve(entity);
				
				// Add the question to the section if it has not been used before and
				// isn't conflicting with a mutually exclusive question already selected
				if ( ! excluded(entity) && ! alreadySelected(entity) && ! missingAnswers(entity) && ! entity.isQuarantined()) {
					// Add it and keep track of the count
					section.addSelection(entity);
		    		if (context.getTestRun()) {
		    			System.out.println("<br>Added to quiz : " + entity.getQuestionId() + "\n");
		    			if (entity.getSupersededBy() != -1) {
		    				System.out.println("<br>This was a superseded question! Bad! Superseded by : " + entity.getSupersededBy());
		    			}
		    		}
					
					// Make note that the question has now been 'deployed'
					if (context.getConfiguration().getMode().contentEquals("PROD")) {
						if ( ! entity.getDeployed()) {
							entity.setDeployed(true);
							entity.save();
						}
					}
				} else {
		    		if (context.getTestRun()) {
		    			System.out.println("<br>Rejected " + entity.getQuestionId() + " because it is excluded/empty/quarantined/already-selected.");
		    			System.out.println("<br>Excluded: " + excluded(entity) 
		    				+ "  Quarantined: " + entity.isQuarantined() 
		    				+ "  Selected: " + alreadySelected(entity)
		    				+ "  Empty: " + missingAnswers(entity)
		    				);
		    		}
				}
			}
    	} else {
    		if (context.getTestRun()) {
            	System.out.println("<br>ERROR : Attempted to add an empty/null section to the quiz. Why?");
    		} else {
        		LOG.info("Attempted to add an empty/null section to the quiz. Why?");
    		}
    	}
    }
    
    // Add a specified number of questions to the selected set of questions in the quiz
    // NOTE: The items are pulled out of the pool in a random order
    // to increase the entropy of each generated quiz.
    public void select(Integer count, Pool pool) {
    	if (section != null) {
	    	if (count <= pool.size()) {
	    		List<Question> result = pool.randomize().getQuestionList();
				for (Question entity : result) {
					// Shortcut out if we've collected enough questions
					if (section.isFull()) break;
					
					// Add the question to the section if it has not been used before and
					// isn't conflicting with a mutually exclusive question already selected
					if ( ! excluded(entity) && !alreadySelected(entity)) {
						// Add it and keep track of the count
						section.addSelection(entity);
						
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
	    		if (context.getTestRun()) {
	    			System.out.println("<br>ERROR : Selection attempted with an undersize pool, wanted " + count + " and only had " + pool.size());
	    		} else {
		    		LOG.info("Selection attempted with an undersize pool, wanted {} and only had {}", count, pool.size());
	    		}
	    	}
    	} else {
    		if (context.getTestRun()) {
        		System.out.println("<br>ERROR : Attempted to add questions outside of a section");
    		} else {
        		LOG.info("Attempted to add questions outside of a section");
    		}
    	}
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
	    			if (context.getTestRun()) {
	            		System.out.println("<br>ERROR : Requested question " + id + " not found");
	    			} else {
	    				LOG.info("Requested question {} not found", id);
	    			}
	    		}
	    	}
	      	exclusives.put(name, group);
    	}
    }
    
    // Get a list of questions by question IDs
    public Pool require(List<Integer> questionIds) {
    	Set<Question> questions = new HashSet<Question>();
    	for (Integer id : questionIds) {
    		Question question = Question.getByQuestionId(new Long(id));
    		if (question != null) {
    			section.addRequired(question);
    		} else {
    			if (context.getTestRun()) {
            		System.out.println("<br>ERROR : Requested question " + id + " not found");
    			} else {
    				LOG.info("Requested question {} not found", id);
    			}
    		}
    	}
    	return new Pool(new ArrayList<Question>(questions));
    }
   
    public Pool questionsWithAny(List<String> atts) {
    	return new Pool().questionsWithAny(atts);
    }
    
    ///////////////////////////////////////
    // Supporting methods
    ///////////////////////////////////////
    
    // Determine if the candidate has already been placed in the selected
    // set for the quiz being generated, and reject it if so.
    private boolean alreadySelected(Question candidate) {
    	if (context.getQuiz().hasQuestion(candidate)) {
    		return true;
    	}
    	if (section.hasQuestion(candidate)) {
    		return true;
    	}

    	return false;
    }
    
    // Determine if the question being considered is absent any answers. It
    // should have never made it into the database, but as a double-check
    // lets at least insure one of those never makes it out the door.
    private boolean missingAnswers(Question candidate) {
    	if (candidate.getAnswers() == null || candidate.getAnswers().size() == 0) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    // Determine if the candidate question should be rejected due to a
    // mutually exclusive question already being in the selected set of
    // questions. 
    private boolean excluded(Question question) {
    	if (question != null && question.getExcludes() != null) {
	    	Set<Question> questionSet = new HashSet<Question>();
	    	questionSet.addAll(context.getQuiz().getQuestions());
	    	questionSet.addAll(section.getQuestions());
	    	for (Question q : questionSet) {
	    		if (question.getExcludes().contains(q.getQuestionId())) {
	        		return true;
	    		}
	    	}
    	}
    	return false;
    }

    // Follow the superseded chain to the current version of the question
    // being processed. 
	private Question resolve(Question question) {
		while (question != null && question.isSuperseded()) {
			long nextId = question.getSupersededBy();
			if ( ! context.getTestRun()) {
				LOG.info("Question {} superseded by {}", question.getQuestionId(), nextId);
			}
			Question tmp = Question.getByQuestionId(nextId);
			if (tmp == null) {
				System.out.println("<br>Found a null entity from resolve(" + question.getQuestionId() + "), shouldn't happen! Fixing.");
				question.setSupersededBy(-1); // This one is no longer superseded, for some reason.
				question.save();
				
				return question; // Return the last good one found (now the last in the chain)
			}
			question = tmp;
		}

		return question;
	}
}
