package org.wingsofcarolina.quiz;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.authentication.AuthenticationExceptionMapper;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.common.RuntimeExceptionMapper;
import org.wingsofcarolina.quiz.domain.Answer;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.SubCategory;
import org.wingsofcarolina.quiz.domain.Type;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.healthcheck.MinimalHealthCheck;
import org.wingsofcarolina.quiz.resources.QuizAPI;
import org.wingsofcarolina.quiz.resources.QuizResource;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class QuizService extends Application<QuizConfiguration> {
	private static final Logger LOG = LoggerFactory.getLogger(QuizService.class);

	public static void main(String[] args) throws Exception {
		LOG.info("Starting : WCFC Quiz Service");
		new QuizService().run(args);
	}

	@Override
	public void initialize(Bootstrap<QuizConfiguration> bootstrap) {
		// bootstrap.addBundle(new AssetsBundle("/doc", "/doc", "index.html","html"));
		bootstrap.addBundle(new TemplateConfigBundle(new TemplateConfigBundleConfiguration()));
		// super.initialize(bootstrap);
	}

	@Override
	public String getName() {
		return "wcfc-quiz";
	}

	@Override
	public void run(QuizConfiguration config, Environment env) throws Exception {
		User user;
		Question question;
		List<Answer> answers;
		
		// Set up the Persistence singleton
		new Persistence().initialize(config.getMongodb());

		// Create the Planez resource object (because it is used in the creation of dummy users)
		QuizResource quiz = new QuizResource(config);

		ObjectMapper mapper = new ObjectMapper();
		List<User> users = new ArrayList<User>();
		
		// Create some initial dummy data
		user = createUser(quiz, "George Scheer", "george.scheer@gmail.com", "REDACTED");
		users.add(user);
		if (user != null) {
			user.addPriv(Privilege.ADMIN);
			user.save();
		}
		user = createUser(quiz, "Dwight Frye", "dfrye@planez.co", "REDACTED");
		users.add(user);
		if (user != null) {
			user.addPriv(Privilege.ADMIN);
			user.save();
		}
		user = createUser(quiz, "Sam Evett", "sam_evett@yahoo.com", "REDACTED");
		users.add(user);
		if (user != null) {
			user.save();
		}
		user = createUser(quiz, "Heinz McArthur", "Heinz@mcarthur.net", "REDACTED");
		users.add(user);
		if (user != null) {
			user.save();
		}

		mapper.writeValue(new FileOutputStream("users.json"), users);

		// Create a question or two
		List<Question> questions = new ArrayList<Question>();
		answers = new ArrayList<Answer>();
		answers.add(new Answer(1, "6500 feet"));
		answers.add(new Answer(2, "2500 feet", true));
		answers.add(new Answer(3, "3545 feet"));
		answers.add(new Answer(4, "1900 feet"));
	    question = new Question(Type.CHOICE, Category.SOP, SubCategory.GENERAL, 
				"What is the minimum length runway permitted by the club SOPs?", 
				"SOP VII", answers, "Don't be stupid. Answer 2 is the right choice");
		question.save();
		questions.add(question);
		
		answers = new ArrayList<Answer>();
		answers.add(new Answer(1, "other random stuff"));
	    question = new Question(Type.BLANK, Category.SOP, SubCategory.GENERAL, 
				"All Club pilots and instructors are bound to observe Club instructional programs and Club {1}__________ as approved by the Board.", 
				"SOP VII", answers, "Don't be stupid. Answer 2 is the right choice");
		question.save();
		questions.add(question);

		answers = new ArrayList<Answer>();
		answers.add(new Answer(1, "emergency procedures"));
		answers.add(new Answer(2, "performance capabilities"));
		answers.add(new Answer(3, "check-outs"));
	    question = new Question(Type.BLANK, Category.SOP, SubCategory.GENERAL, 
	    		"Regardless of this test, instructors are expected to assure knowledge of appropriate " +
	    		"V-speeds, aircraft servicing requirements, {1}__________, " +
	    		"{2}__________ of the aircraft for all {3} __________, and for flight training courses.",
	    		"SOP VII", answers, "Don't be stupid. Answer 2 is the right choice");
		question.save();
		questions.add(question);
		
		mapper.writeValue(new FileOutputStream("questions.json"), questions);
		
		// Set exception mappers
		env.jersey().register(new AuthenticationExceptionMapper());
//        env.jersey().register(new RuntimeExceptionMapper());
		
		// Now finish setting up the API
		env.jersey().register(quiz);
		env.jersey().register(new QuizAPI(config));
		env.healthChecks().register("check", new MinimalHealthCheck());
	}
	
	private User createUser(QuizResource quiz, String name, String email, String password) {
		User user = null;
		if (User.getByEmail(email) == null) {
			user = quiz.addUser(name, email, password, Privilege.USER);
			user.save();
		}
		return user;
	}
}
