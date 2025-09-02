package org.wingsofcarolina.quiz;

import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
// Temporarily disabled - needs Dropwizard 4.x compatible version
// import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
// import de.thomaskrille.dropwizard_template_config.TemplateConfigBundleConfiguration;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.forms.MultiPartBundle;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.knowm.dropwizard.sundial.SundialBundle;
import org.knowm.dropwizard.sundial.SundialConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.authentication.AuthenticationExceptionMapper;
import org.wingsofcarolina.quiz.authentication.HashUtils;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.healthcheck.MinimalHealthCheck;
import org.wingsofcarolina.quiz.resources.QuizAPI;
import org.wingsofcarolina.quiz.resources.QuizResource;
import org.wingsofcarolina.quiz.resources.Slack;

public class QuizService extends Application<QuizConfiguration> {

  private static final Logger LOG = LoggerFactory.getLogger(QuizService.class);

  public static void main(String[] args) throws Exception {
    LOG.info("Starting : WCFC Quiz Service");
    if (args.length < 2) {
      new QuizService().run(new String[] { "server", "configuration.yml" });
    } else {
      new QuizService().run(args);
    }
  }

  @Override
  public void initialize(Bootstrap<QuizConfiguration> bootstrap) {
    // Enable environment variable substitution
    bootstrap.setConfigurationSourceProvider(
      new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(),
        new EnvironmentVariableSubstitutor(false)
      )
    );

    // bootstrap.addBundle(new AssetsBundle("/doc", "/doc", "index.html","html"));
    // Temporarily disabled - needs Dropwizard 4.x compatible version
    // bootstrap.addBundle(new TemplateConfigBundle(new TemplateConfigBundleConfiguration()));
    bootstrap.addBundle(new AssetsBundle("/assets/", "/static"));
    bootstrap.addBundle(new MultiPartBundle());
    bootstrap.addBundle(
      new SundialBundle<QuizConfiguration>() {
        @Override
        public SundialConfiguration getSundialConfiguration(
          QuizConfiguration configuration
        ) {
          return configuration.getSundialConfiguration();
        }
      }
    );
  }

  @Override
  public String getName() {
    return "wcfc-quiz";
  }

  @Override
  public void run(QuizConfiguration config, Environment env) throws Exception {
    // Get the startup date/time in GMT
    SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
    dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));

    // Set up Slack communications
    Slack slack = new Slack(config);
    slack.sendMessage(
      "WCFC Quiz System restarted at " + dateFormatGmt.format(new Date())
    );
    slack.sendMessage("WCFC Quiz System running in '" + config.getMode() + "' mode.");

    LOG.info("Java Version : {}", System.getProperty("java.version"));

    // Set up the Persistence singleton
    new Persistence().initialize(config.getMongodb());

    // Create the Planez resource object (because it is used in the creation of dummy users)
    QuizResource quiz = new QuizResource(config);

    // Set exception mappers
    env.jersey().register(new AuthenticationExceptionMapper());
    // Log all the 404 errors
    env.jersey().register(NotFoundFilter.class);
    //if (config.getMode().contentEquals("PROD")) {
    env.jersey().register(new RuntimeExceptionMapper());
    //}

    // Now finish setting up the API
    env.jersey().register(quiz);
    env.jersey().register(new QuizAPI(config));
    env.healthChecks().register("check", new MinimalHealthCheck());

    // Now create two mandatory default users, if they don't exist
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
}
