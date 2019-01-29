package org.wingsofcarolina.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.authentication.AuthenticationExceptionMapper;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.common.RuntimeExceptionMapper;
import org.wingsofcarolina.quiz.domain.Lesson;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.Syllabus;
import org.wingsofcarolina.quiz.domain.Type;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.healthcheck.MinimalHealthCheck;
import org.wingsofcarolina.quiz.resources.QuizAPI;
import org.wingsofcarolina.quiz.resources.QuizResource;

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
		// Set up the Persistence singleton
		new Persistence().initialize(config.getMongodb());

		// Create the Planez resource object (because it is used in the creation of dummy users)
		QuizResource quiz = new QuizResource(config);

		// Create some initial dummy data
		User instructor1 = createUser(quiz, "George Scheer", "george.scheer@gmail.com", "REDACTED", Privilege.USER);
		if (instructor1 != null) {
			instructor1.addPriv(Privilege.INSTRUCTOR);
			instructor1.save();
		}
		User instructor2 = createUser(quiz, "Dwight Frye", "dfrye@planez.co", "REDACTED", Privilege.ADMIN);
		if (instructor2 != null) {
			instructor2.addPriv(Privilege.ADMIN);
			instructor2.addPriv(Privilege.INSTRUCTOR);
			instructor2.save();
		}
		
		// Set exception mappers
		env.jersey().register(new AuthenticationExceptionMapper());
//        env.jersey().register(new RuntimeExceptionMapper());
		
		// Now finish setting up the API
		env.jersey().register(quiz);
		env.jersey().register(new QuizAPI(config));
		env.healthChecks().register("check", new MinimalHealthCheck());
	}
	
	private User createUser(QuizResource quiz, String name, String email, String password, Privilege priv) {
		User user = null;
		if (User.getByEmail(email) == null) {
			user = quiz.addUser(name, email, password, priv);
			user.save();
		}
		return user;
	}
}
