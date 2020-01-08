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
import org.wingsofcarolina.quiz.domain.Attribute;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
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
    public void start(Integer sectionCount, String sectionName) {
    	if (context.getTestRun()) {
        	System.out.println("<br>INFO  : Starting new section : " + sectionName);
    	} else {
        	LOG.info("Starting new section : {}", sectionName);
    	}
    	this.sectionCount = sectionCount;
    	section = new HashSet<Question>();
    }
    
    // Apply the collection set of questions to the quiz
    public void end(String name) {
    	if (context.getTestRun()) {
        	System.out.println("<br>INFO  : Applying collected section : " + name);
    	} else {
        	LOG.info("Applying collected section : {}", name);
    	}
    	if (section != null) {
	    	List<Question> questions = new ArrayList<Question>();
	    	questions.addAll(section);
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
				
				// Add the question to the section if it has not been used before and
				// isn't conflicting with a mutually exclusive question already selected
				if ( ! excluded(entity) && ! alreadySelected(entity) && ! missingAnswers(entity) && ! entity.isQuarantined()) {
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
				} else {
		    		if (context.getTestRun()) {
		    			System.out.println("<br>Rejected " + entity.getQuestionId() + "because it is excluded/empty/quarantined/already-selected.");
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
    public void select(Integer count, List<Question> pool) {
    	if (section != null) {
	    	if (count <= pool.size()) {
	    		List<Question> result = randomize(pool);
				for (Question entity : result) {
					// Shortcut out if we've collected enough questions
					if (sectionCount == 0) break;
					
					// Add the question to the section if it has not been used before and
					// isn't conflicting with a mutually exclusive question already selected
					if ( ! excluded(entity) && !alreadySelected(entity)) {
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
    
    // Filter the list returning only those questions which contain the 
    // indicated attribute.
    public List<Question> filterOnly(String attribute, List<Question> pool) {
    	List<Question> result = new ArrayList<Question>();
    	for (Question question: pool) {
    		if (question.getAttributes().contains(attribute)) {
    			result.add(question);
    		}
    	}
    	return result;
    }
    
    // Get a list of questions by question IDs
    public List<Question> include(List<Integer> questionIds) {
    	Set<Question> questions = new HashSet<Question>();
    	for (Integer id : questionIds) {
    		Question question = Question.getByQuestionId(new Long(id));
    		if (question != null) {
    			questions.add(question);
    		} else {
    			if (context.getTestRun()) {
            		System.out.println("<br>ERROR : Requested question " + id + " not found");
    			} else {
    				LOG.info("Requested question {} not found", id);
    			}
    		}
    	}
    	return new ArrayList<Question>(questions);
    }
    
    // Get a list of questions by category
    public List<Question> questions() {
    	Category category = context.getQuiz().getCategory();
    	List<Question> result = Question.getSelected(category);
		return result;
    }
    
    // Get a list of questions by category, filtered by attributes
    public List<Question> questionsWithAny(List<String> atts) {
    	Category category = context.getQuiz().getCategory();
    	List<Question> result = new ArrayList<Question>();
		if (atts.contains(Attribute.ANY)) {
			result = Question.getSelected(category);
		} else {
	    	for (Question question : Question.getSelected(category)) {
	    		for (String attribute : question.getAttributes()) {
	    			if (atts.contains(attribute)) {
	    				result.add(question);
	    				break;
	    			}
	    		}
	    	}
		}
		return result;
    }
    
    ///////////////////////////////////////
    // Supporting methods
    ///////////////////////////////////////
    private List<Question> randomize(List<Question> pool) {
    	List<Question> result = new ArrayList<Question>();
    	
    	if (pool.size() > 0) {
			do {
				int size = pool.size();
				if (size > 0) {
					int pick = (int)(Math.random() * size);
					result.add(pool.remove(pick));
				} else {
					if (context.getTestRun()) {
						System.out.println("<br>ERROR : Why did the pool get to zero???");
					} else {
						LOG.info("Why did the pool get to zero???");
					}
				}
			} while (pool.size() > 0);
    	}
    	
    	return result;
    }
    
    // Determine if the candidate has already been placed in the selected
    // set for the quiz being generated, and reject it if so.
    private boolean alreadySelected(Question candidate) {
    	if (context.getQuiz().hasQuestion(candidate)) {
			if (context.getTestRun()) {
				System.out.println("<br>INFO : Found a duplicate in Quiz, skipping " + candidate.getQuestionId());
			} else {
				LOG.info("Found a duplicate, skipping {}", candidate.getQuestionId());
			}
    		return true;
    	}
    	long id = candidate.getQuestionId();
		for (Question question : section) {
			if (question.getQuestionId() == id) {
				if (context.getTestRun()) {
					System.out.println("<br>INFO : Found a duplicate, skipping " + id);
				} else {
					LOG.info("Found a duplicate, skipping {}", id);
				}
				return true;
			}
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
    private boolean excluded(Question candidate) {
    	if (candidate.getExcludes() != null) {
	    	Set<Question> questionSet = new HashSet<Question>();
	    	questionSet.addAll(context.getQuiz().getQuestions());
	    	questionSet.addAll(section);
	    	for (Question question : questionSet) {
	    		if (candidate.getExcludes().contains(question.getQuestionId())) {
	    			if (context.getTestRun()) {
	    				System.out.println("<br>INFO : Found a mutual exclusion in quiz, skipping " + candidate.getQuestionId());
	    			} else {
	    				LOG.info("Found a mutual exclusion in quiz, skipping {}", candidate.getQuestionId());
	    			}
	        		return true;
	    		}
	    	}
    	}
    	return false;
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
}
