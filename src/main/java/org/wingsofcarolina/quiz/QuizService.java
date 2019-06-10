package org.wingsofcarolina.quiz;

import org.knowm.dropwizard.sundial.SundialBundle;
import org.knowm.dropwizard.sundial.SundialConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.authentication.AuthenticationExceptionMapper;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.common.RuntimeExceptionMapper;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.healthcheck.MinimalHealthCheck;
import org.wingsofcarolina.quiz.resources.QuizAPI;
import org.wingsofcarolina.quiz.resources.QuizResource;
import org.wingsofcarolina.quiz.resources.Slack;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class QuizService extends Application<QuizConfiguration> {
	private static final Logger LOG = LoggerFactory.getLogger(QuizService.class);

	public static void main(String[] args) throws Exception {
		LOG.info("Starting : WCFC Quiz Service");
        if (args.length < 2) {
            new QuizService().run(new String[]{"server", "configuration.ftl"});
        } else {
            new QuizService().run(args);
        }
	}

	@Override
	public void initialize(Bootstrap<QuizConfiguration> bootstrap) {
		// bootstrap.addBundle(new AssetsBundle("/doc", "/doc", "index.html","html"));
		bootstrap.addBundle(new TemplateConfigBundle(new TemplateConfigBundleConfiguration()));
        bootstrap.addBundle(new AssetsBundle("/assets/", "/static"));
        bootstrap.addBundle(new SundialBundle<QuizConfiguration>() {

            @Override
            public SundialConfiguration getSundialConfiguration(QuizConfiguration configuration) {
              return configuration.getSundialConfiguration();
            }
          });
        }

	@Override
	public String getName() {
		return "wcfc-quiz";
	}

	@Override
	public void run(QuizConfiguration config, Environment env) throws Exception {
		// Set up Slack communications
		new Slack(config);
		
		// Set up the Persistence singleton
		new Persistence().initialize(config.getMongodb());

		// Create the Planez resource object (because it is used in the creation of dummy users)
		QuizResource quiz = new QuizResource(config);

		// Set exception mappers
		env.jersey().register(new AuthenticationExceptionMapper());
		if (config.getMode().contentEquals("PROD")) {
			env.jersey().register(new RuntimeExceptionMapper());
		}
		
		// Now finish setting up the API
		env.jersey().register(quiz);
		env.jersey().register(new QuizAPI(config));
		env.healthChecks().register("check", new MinimalHealthCheck());
	}
}
