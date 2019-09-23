package org.wingsofcarolina.populate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.wingsofcarolina.quiz.authentication.HashUtils;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.domain.Answer;
import org.wingsofcarolina.quiz.domain.Attribute;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Recipe;
import org.wingsofcarolina.quiz.domain.Section;
import org.wingsofcarolina.quiz.domain.Selection;
import org.wingsofcarolina.quiz.domain.Type;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.resources.Quiz;
import org.wingsofcarolina.quiz.resources.QuizResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thedeanda.lorem.Lorem;
import com.thedeanda.lorem.LoremIpsum;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

import org.apache.commons.collections4.ListUtils;
import org.slf4j.LoggerFactory;

public class Populate {

	Lorem lorem;
	Persistence persistence;
	ObjectMapper objectMapper = new ObjectMapper();

	public Populate() {
		// Set logging level for MongoDB
		((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("org.mongodb.driver").setLevel(Level.ERROR);
		// Instantiate/initialize the persistence layer
		String mongodb = System.getenv("MONGODB");
		System.out.println("MongoDB : " + mongodb);
		persistence = new Persistence().initialize(mongodb);
		// Get a random data source instance
		lorem = LoremIpsum.getInstance();
	}
	
	private void run() throws FileNotFoundException, IOException {
//		resetQuestionValue();
		createUsers();
		createRecipes();
		createFARQuestions();
		sopQuestions();
		createAirplaneQuestions(Category.C152);
		createAirplaneQuestions(Category.C172);
		createAirplaneQuestions(Category.PA28);
		createAirplaneQuestions(Category.M20J);
		
		System.out.println("Done.");
	}
	
	private void resetQuestionValue() {
		int count = 0;
		
		List<Question> questions = Question.getAllQuestions();
		
		for (Question question : questions) {
			question.setDeployed(false);
			question.setSupercededBy(-1);
			question.save();
			count++;
		}
		
		System.out.println("Number of questions modified : " + count);
	}
	
	private void createUsers() {
		// Create some initial dummy data
		User user = createUser("Dwight Frye", "dfrye@planez.co", "REDACTED");
		if (user != null) {
			user.addPriv(Privilege.ADMIN);
			user.save();
		}
		user = createUser("George Scheer", "george.scheer@gmail.com", "REDACTED");
		if (user != null) {
			user.addPriv(Privilege.ADMIN);
			user.save();
		}
	}
	
	private User createUser(String name, String email, String password) {
		User user = null;
		if (User.getByEmail(email) == null) {
			user = addUser(name, email, password, Privilege.USER);
			user.save();
		}
		return user;
	}
	
	public User addUser(String name, String email, String password, Privilege priv) {
		String hashedPw = null;
		try {
			if (password != null) {
				hashedPw = HashUtils.generateStrongPasswordHash(password);
			}
			
			User user = User.getByEmail(email);
			if (user == null) {
				if (hashedPw == null) {
					user = new User(email);
				} else {
					user = new User(email, hashedPw);
				}
				if (name == null) {
					name = "none";
				}
				user.setName(name);
				user.addPriv(priv);
				user.save();
				System.out.println("New user  : " + user);
			} else {
				System.out.println("User '" + user.getEmail() + "' already exists.");
			}

			return user;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private void createRecipes() {
		createRecipe(Quiz.QuizType.FAR, Arrays.asList(Attribute.ANY));
		createRecipe(Quiz.QuizType.SOP_STUDENT, Arrays.asList(Attribute.ANY));
		createRecipe(Quiz.QuizType.SOP_PILOT, Arrays.asList(Attribute.ANY));
		createRecipe(Quiz.QuizType.SOP_INSTRUCTOR, Arrays.asList(Attribute.ANY));
		createRecipe(Quiz.QuizType.C152, Arrays.asList(Attribute.GENERAL, Attribute.EASY));
		createRecipe(Quiz.QuizType.C172, Arrays.asList(Attribute.GENERAL));
		createRecipe(Quiz.QuizType.PA28, Arrays.asList(Attribute.GENERAL));
		List<Long> required = new ArrayList<Long>();
		required.add((long) 1960);
		required.add((long) 1961);
		required.add((long) 1962);
		required.add((long) 1963);
		required.add((long) 3000);
		createRecipe(Quiz.QuizType.M20J, Arrays.asList(Attribute.GENERAL), required);
	}
	
	private void createRecipe(Quiz.QuizType quizType, List<String> attributes) {
		createRecipe(quizType, attributes, null);
	}
	
	private Recipe createRecipe(Quiz.QuizType quizType, List<String> attributes, List<Long> required) {
		Recipe recipe = new Recipe(quizType);
		Section section = new Section("only");
		recipe.addSection(section);
		section.addSelection(new Selection(5, attributes));
		if (required != null) {
			section.setRequired(required);
		}
		recipe.save();
		
		if (quizType == Quiz.QuizType.M20J) {
			try {
				objectMapper.writeValue(new File("recipe.json"), recipe);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return recipe;
	}
	
	private void createFARQuestions() {
		List<String> attributes = new ArrayList<String>();

		for (int i = 0; i < 100; i++) {
			attributes.clear();
			multipleChoice(Category.FAR, Arrays.asList(Attribute.ANY));
		}
	}

	private void createAirplaneQuestions(Category category) {
		List<String> attributes = new ArrayList<String>();

		for (int i = 0; i < 200; i++) {
			attributes.clear();
			multipleChoice(category, Attribute.aircraft_attributes);
		}
	}

	private void multipleChoice(Category category) {
		multipleChoice(category, null);
	}
	
	private void multipleChoice(Category category, List<String> attributes) {
		Question question;
		List<Answer> answers;
		List<String> attributeList = null;
		
		if (attributes != null) {
			attributeList = Arrays.asList(attributes.get(pick(attributes.size())));
		}

		attributeList = ListUtils.union(attributeList, Arrays.asList(Attribute.difficulty_attributes.get(pick(3))));
		attributeList.add(Attribute.TEST);

		answers = new ArrayList<Answer>();
		answers.add(new Answer(getWords(5, 15)));
		answers.add(new Answer(getWords(5, 15)));
		answers.add(new Answer(getWords(5, 15)));
		answers.add(new Answer(getWords(5, 15)));
		answers.get(pick(4)).setCorrect(true);
		// Select some references
	    question = new Question(Type.CHOICE, category, attributeList, 
				getWords(10, 20), lorem.getWords(1, 3), answers, lorem.getParagraphs(1, 2));
		question.save();
	}
	
	private void sopQuestions() {
		Question question;
		List<Answer> answers;

		for (int i = 0; i < 100; i++) {
			// Fabricate attributes
			List<String> attributes = new ArrayList<String>();
			attributes.add(Attribute.sop_attributes.get(pick(Attribute.sop_attributes.size())));
			attributes.add(Attribute.level_attributes.get(pick(Attribute.level_attributes.size())));
			attributes.add(Attribute.TEST);

			answers = new ArrayList<Answer>();
			int count = pick(4) + 1;
			for (int j = 0; j < count; j++) {
				answers.add(new Answer(getWords(5, 15)));
			}
			int pick = pick(count);
			answers.get(pick).setCorrect(true);
		    question = new Question(Type.BLANK, Category.SOP, attributes, 
					getWordsWithBlanks(count, 10, 20), lorem.getWords(1, 3), answers, lorem.getParagraphs(1, 2));
			question.save();
		}
	}
	
	private String getWords(int min, int max) {
		String str = lorem.getWords(min, max);
		str = str.substring(0, 1).toUpperCase() + str.substring(1);
		return str + ".";
	}

	private String getWordsWithBlanks(int count, int min, int max) {
		String str = lorem.getWords(min, max);
		str = str.substring(0, 1).toUpperCase() + str.substring(1);
		
		// Insert blanks
		int increment = str.length() / count;
		int position = increment / 2;
		for (int i = 0; i < count; i++) {
			str = str.substring(0 , position) + " __________ " + str.substring(position);
			position += increment + 12;
		}
		return str + ".";
	}

	private int pick(int count) {
		return (int)(Math.random() * count);
	}
	
	public static void main(String[] args) {
		Populate populate = new Populate();
		try {
			populate.run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
