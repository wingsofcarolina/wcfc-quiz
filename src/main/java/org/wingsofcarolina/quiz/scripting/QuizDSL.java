package org.wingsofcarolina.quiz.scripting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Section;
import org.wingsofcarolina.quiz.resources.Quiz;
import org.wingsofcarolina.quiz.resources.QuizContext;

import groovy.lang.Script;

public abstract class QuizDSL extends Script {
	private static final Logger LOG = LoggerFactory.getLogger(QuizDSL.class);

    private QuizContext context;
	private Section section = null;

    public QuizContext getContext() {
        return context;
    }

    public void setContext(QuizContext context) {
        this.context = context;
    }

    // Implement classic Groovy method/property missing behavior
    public Object propertyMissing(String name) {
        if (context.getTestRun()) {
            System.out.println("propertyMissing called with ---> " + name + "<br>");
        } else {
            LOG.info("propertyMissing called with ---> {}", name);
        }
        return null;
    }
    public Object methodMissing(String name, Object args) {
        List<Object> argsList = Arrays.asList((Object[]) args);
        if (context.getTestRun()) {
        	System.out.println("ERROR : methodMissing called for : " + name + "</br>");
        } else {
        	LOG.info("methodMissing called for ---> {}", name);
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
    
    // Set quiz global values
    public void quiz(Integer count) {
    	Quiz quiz = context.getQuiz();
    	quiz.setMaxCount(count);
    }
    public void quiz(Integer count, String title) {
    	quiz(count);
    	context.getQuiz().setQuizName(title);
    }
   
    // Start a "section" of the quiz
    public void start(Integer questionCount, String sectionName) {
    	if (context.getTestRun()) {
        	System.out.println("### Starting new section : " + sectionName + "<br>");
    	} else {
        	LOG.info("Starting new section : {}", sectionName);
    	}
    	this.section = new Section(questionCount, sectionName);
    }
    
    // Apply the collection set of questions to the quiz
    public void end(String name) {
    	List<Question> questions = section.getQuestions();
    	if (context.getTestRun()) {
        	System.out.println("### Applying collected selections : " + name + "(" + questions.size() + ")<br>");
    	}
    	if (section != null) {
	    	Quiz quiz = context.getQuiz();
	    	for (Question question : questions) {
	    		quiz.addQuestion(question);
			
				// Make note that the question has now been 'deployed'
				if ( ! context.getTestRun() && ! question.getDeployed()) {
					question.setDeployed(true);
					question.save();
				}
				
				// Stop when the quiz is full
				if (quiz.getMaxCount() != null && quiz.getQuestions().size() >= quiz.getMaxCount()) {
					break;
				}
	    	}
    	} else {
    		if (context.getTestRun()) {
            	System.out.println("ERROR : Attempted to add an empty/null section to the quiz. Why?<br>");
    		} else {
        		LOG.info("Attempted to add an empty/null section to the quiz. Why?");
    		}
    	}
    }
    
    // Add questions to the selected set of questions in the quiz
    // NOTE: The items are pulled out of the Pool pool in a random order
    // to increase the entropy of each generated quiz.
    public void select(Pool pool) {
    	select(null, pool);
    }
    
    // Add a specified number of questions to the selected set of questions in the quiz
    // NOTE: The items are pulled out of the pool in a random order
    // to increase the entropy of each generated quiz.
    public void select(Integer count, Pool pool) {
    	int numSelected = 0;
    	
    	// If we asked for no specific count, then the pool is 
    	// assumed to be the "right size".
    	if (count == null) {
    		count = pool.size();
    	}
		if (context.getTestRun()) {
			System.out.println("New selection of count == " + count + "<br>");
		}
    	if (section != null && pool != null) {
	    	if (count <= pool.size()) {
	    		List<Question> result = pool.randomize().getQuestionList();
				for (Question entity : result) {
					// Shortcut out if we've collected enough questions
					if (section.isFull() || numSelected == count) break;
					
					// Resolve the actual question (i.e. follow the superseded chain)
					entity = resolve(entity);
					
					// Add the question to the section if it has not been used before and
					// isn't conflicting with a mutually exclusive question already selected
					if ( ! excluded(entity) && !alreadySelected(entity)) {
						// Add it and keep track of the count
						section.addSelection(entity);
						numSelected++;
			    		if (context.getTestRun()) {
			    			System.out.println("Added to selections : " + entity.getQuestionId() + "<br>");
			    		}
					}  else {
			    		if (context.getTestRun()) {
			    			System.out.println("Rejected " + entity.getQuestionId() + " because it is excluded/empty/quarantined/already-selected.<br>");
			    			System.out.println("Excluded: " + excluded(entity) 
			    				+ "  Quarantined: " + entity.isQuarantined() 
			    				+ "  Selected: " + alreadySelected(entity)
			    				+ "  Empty: " + missingAnswers(entity)
			    				+ "<br>");
			    		}
					}
				}
	    	} else {
	    		if (context.getTestRun()) {
	    			System.out.println("ERROR : Selection attempted with an undersize pool, wanted " + count
	    					+ " and only had " + pool.size()
	    					+ " in section " + section.getName()
	    					+ "<br>");
	    		} else {
		    		LOG.info("Selection attempted with an undersize pool, wanted {} and only had {} in section {}",
		    				count, pool.size(), section.getName());
	    		}
	    	}
    	} else {
    		if (context.getTestRun()) {
        		System.out.println("ERROR : Attempted to add questions outside of a section.<br>");
    		} else {
        		LOG.info("Attempted to add questions outside of a section");
    		}
    	}
    }
    
    // Add to the section (not pool) all (resolved) questions listed
    // in the question ID list provided.
    public void require(Integer questionId) {
    	List<Integer> list = new ArrayList<Integer>();
    	list.add(questionId);
    	require(list);
    }
    public void require(List<Integer> questionIds) {
    	for (Integer id : questionIds) {
    		Question question = Question.getByQuestionId(new Long(id));
    		question = resolve(question);
    		if (question != null) {
    			section.addRequired(question);
	    		if (context.getTestRun()) {
	    			System.out.println("Added to quiz : " + question.getQuestionId() + "<br>");
	    		}
    		} else {
    			if (context.getTestRun()) {
            		System.out.println("ERROR : Requested question " + id + " not found<br>");
    			} else {
    				LOG.info("Requested question {} not found", id);
    			}
    		}
    	}
    }
   
    // Return a pool of questions which match any of the supplied attributes. This
    // can result in a very large question pool potentially spanning multiple aircraft
    // types. This is essentially an inclusive OR operation on the attributes.Be ware.
    public Pool getWithAny(String attribute) {
    	List<String> list = new ArrayList<String>();
    	list.add(attribute);
    	return getWithAny(list);
    }
    public Pool getWithAny(List<String> attributes) {
    	return new Pool().getWithAny(attributes);
    }
    
    // Return a pool of questions which match ALL of the supplied attributes. This
    // is essentially an inclusive AND operation on the attributes.
    public Pool getWithAll(String attribute) {
    	List<String> list = new ArrayList<String>();
    	list.add(attribute);
    	return getWithAll(list);
    }
    public Pool getWithAll(List<String> attributes) {
    	return new Pool().getWithAll(attributes);
    }
    
    // Filter a pool such that all questions with the listed attributes are 
    // included in the resulting pool.
    public Pool filterIn(Pool pool, String attribute) {
    	List<String> list = new ArrayList<String>();
    	list.add(attribute);
    	return filterIn(pool, list);
    }
    public Pool filterIn(Pool pool, List<String> attributes) {
    	List<Question> list = new ArrayList<Question>();
    	
    	for (ListIterator<Question> iter = pool.getQuestionList().listIterator(); iter.hasNext(); ) {
    	    Question question = iter.next();
    	    for (String attribute : attributes) {
    	    	if (question.hasAttribute(attribute)) {
    	    		list.add(question);
    	    		break;
    	    	}
    	    }
    	}
    	return new Pool(list);
    }
    
    // Filter a pool such that all questions with the listed attributes are 
    // excluded in the resulting pool.
    public Pool filterOut(Pool pool, String attribute) {
    	List<String> list = new ArrayList<String>();
    	list.add(attribute);
    	return filterOut(pool, list);
    }
    public Pool filterOut(Pool pool, List<String> attributes) {
    	List<Question> list = new ArrayList<Question>();
    	
    	for (ListIterator<Question> iter = pool.getQuestionList().listIterator(); iter.hasNext(); ) {
    	    Question question = iter.next();
    	    boolean found = false;
    	    for (String attribute : attributes) {
    	    	if (question.hasAttribute(attribute)) {
    	    		found = true;
    	    	}
    	    }
    	    if ( ! found ) {
	    		list.add(question);
    	    }
    	}
    	return new Pool(list);
    }
    
    // Combine two pools such that the resulting pool is treated as a set,
    // in that if a question exists in both pools it is reflected only once
    // in the resulting combined pool.
    public Pool combine( Pool pool1, Pool pool2) {
    	Set<Question> set = new HashSet<Question>();

    	set.addAll(pool1.getQuestionList());
    	set.addAll(pool2.getQuestionList());
    	List<Question> questions = new ArrayList<Question>();
    	questions.addAll(set);
        
    	return new Pool(questions);
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
