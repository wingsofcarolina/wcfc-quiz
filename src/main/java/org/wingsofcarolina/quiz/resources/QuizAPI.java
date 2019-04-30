package org.wingsofcarolina.quiz.resources;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.QuizConfiguration;
import org.wingsofcarolina.quiz.authentication.AuthUtils;
import org.wingsofcarolina.quiz.authentication.AuthenticationException;
import org.wingsofcarolina.quiz.authentication.HashUtils;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.common.Flash;
import org.wingsofcarolina.quiz.common.Pages;
import org.wingsofcarolina.quiz.common.Templates;
import org.wingsofcarolina.quiz.domain.Answer;
import org.wingsofcarolina.quiz.domain.Attribute;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.Type;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.responses.LoginResponse;
import org.wingsofcarolina.quiz.responses.RedirectResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.thomaskrille.dropwizard_template_config.redist.freemarker.template.Configuration;
import de.thomaskrille.dropwizard_template_config.redist.freemarker.template.Template;
import de.thomaskrille.dropwizard_template_config.redist.freemarker.template.TemplateException;
import de.thomaskrille.dropwizard_template_config.redist.freemarker.template.TemplateExceptionHandler;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/**
 * @author dwight
 *
 */
@Path("/api")
public class QuizAPI {
	private static final Logger LOG = LoggerFactory.getLogger(QuizAPI.class);
	@SuppressWarnings("unused")
	private QuizConfiguration config;
	private AuthUtils authUtils;
	private Configuration freemarker;	// FreeMarker configuration
	private ObjectMapper objectMapper;
	private String dataDir;
	private String questionDir;

	public QuizAPI(QuizConfiguration config) throws IOException {
		this.config = config;
		authUtils = new AuthUtils();
		objectMapper = new ObjectMapper();
		dataDir = config.getDataDirectory();
		questionDir = dataDir + "/questions";
		
		// Create your Configuration instance, and specify if up to what FreeMarker
		// version (here 2.3.27) do you want to apply the fixes that are not 100%
		// backward-compatible. See the Configuration JavaDoc for details.
		freemarker = new Configuration(Configuration.VERSION_2_3_22);

		// Specify the source where the template files come from. Here I set a
		// plain directory for it, but non-file-system sources are possible too:
		// TODO: Make this a configuration option
		freemarker.setDirectoryForTemplateLoading(new File(config.getTemplates()));
		freemarker.setTemplateUpdateDelay(0);  // TODO: Change this for "production"

		// Set the preferred charset template files are stored in. UTF-8 is
		// a good choice in most applications:
		freemarker.setDefaultEncoding("UTF-8");

		// Sets how errors will appear.
		// During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
		freemarker.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

		// Don't log exceptions inside FreeMarker that it will thrown at you anyway:
		freemarker.setLogTemplateExceptions(false);
	}

	@POST
	@Path("login")
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(@FormParam("email") String email, @FormParam("password") String password,
			@FormParam("type") String type) throws Exception {
		
		String token = null;

		if (email == null || password == null) {
			Flash.add(Flash.Code.ERROR, "Either email or password missing, try again");
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}

		String storedPassword = getUserCredentials(email); // users.get(user);
		LOG.debug("Stored Password : {}", storedPassword);
		try {
			if (storedPassword == null || !HashUtils.validatePassword(password, storedPassword)) {
				LOG.info("User not found : {}", email);
				Flash.add(Flash.Code.ERROR, "User not found, try again");
				return new RedirectResponse(Pages.LOGIN_PAGE).build();
			}
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
			Flash.add(Flash.Code.ERROR, "User not found, try again");
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}

		// Update the user
		User user = User.getByEmail(email);
		token = authUtils.generateToken(user);
		user.setToken(token);
		user.save();
		LOG.info("Logged in  : {}", user);

		NewCookie cookie = new NewCookie("quiz.token", token, "/", "", "Quiz Login Token", -1, false);
		return new LoginResponse(cookie).build();
	}

	@GET
	@Path("logout")
	@Produces(MediaType.APPLICATION_JSON)
	public Response logout(@CookieParam("quiz.token") Cookie cookie) throws Exception {
		if (cookie != null) {
			Jws<Claims> claims;
			try {
				claims = authUtils.validateUser(cookie.getValue());
				User user = User.getWithClaims(claims);
				user.setToken(null);
				user.save();
			} catch (AuthenticationException e) {
				// Ignore, since we are logging out anyway
			}
		}
		NewCookie newCookie = new NewCookie("quiz.token", "", "/", "", "Quiz Login Token", 0, false);
		return new RedirectResponse(Pages.LOGIN_PAGE).cookie(newCookie).build();
	}

	@POST
	@Path("register")
	@Produces(MediaType.APPLICATION_JSON)
	public Response register(@FormParam("action") String action,
			@FormParam("fullname") String fullname,
			@FormParam("email") String email,
			@FormParam("password") String password,
			@FormParam("passwordVerify") String passwordVerify,
			@FormParam("name") String name,
			@FormParam("phone") String phone,
			@FormParam("member") String member)
			throws Exception {

		if (action.equals("Register")) {
			User user = null;
			String token = null;

			if (email == null || password == null) {
				Flash.add(Flash.Code.ERROR, "No email or password provided, try again");
				return new RedirectResponse(Pages.REGISTER_PAGE).build();
			} else if (! password.equals(passwordVerify)) {
				Flash.add(Flash.Code.ERROR, "Passwords do not match");
				return new RedirectResponse(Pages.REGISTER_PAGE).build();
			}

			if (User.getByEmail(email) == null) {
				user = QuizResource.instance().addUser(name, email, password, Privilege.USER);
				user.save();
			}

			// Update the user
			token = authUtils.generateToken(user);
			user.setToken(token);
			user.save();
			LOG.info("Registered  : {}", user);

			NewCookie cookie = new NewCookie("quiz.token", token, "/", "", "Quiz Login Token", -1, false);
			return new LoginResponse(cookie).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@POST
	@Path("retrieve")
	@Produces("text/html")
	public Response retrieve(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("quizId") long quizId,
			@FormParam("type") String type) throws AuthenticationException, IOException {
		
		String output = "";
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
		User user = User.getWithClaims(claims);
		LOG.info("Quiz {} retrieved by {}", quizId, user.getFullname());
		
		Record record = Record.getByQuizId(quizId);
		if (record != null) {
			Quiz quiz = Quiz.quizFromRecord(record);
			if (type.equals("Key")) {
				output = renderFreemarker(Templates.KEY, quiz).toString();
			} else {
				output = renderFreemarker(Templates.QUIZ, quiz).toString();
			}
			return Response.ok().entity(output).build();
		}
		Flash.add(Flash.Code.ERROR, "Requested quiz not found.");
		return new RedirectResponse(Pages.HOME_PAGE).build();
	}

	@GET
	@Path("backupQuestions")
	public Response backupQuestions(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException {
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		// Make the directory if it doesn't exist
		new File(dataDir).mkdir();
		new File(questionDir).mkdir();
		
		List<Question> questions = Question.getAllQuestions();
		for (Question question : questions) {
			String name = questionDir + "/" + question.getQuestionId() + ".json";
			try {
				objectMapper.writeValue(new File(name), question);
			} catch (IOException e) {
				LOG.info("IOException writing question {}", name, e);
			}
		}
		
		return new RedirectResponse(Pages.HOME_PAGE).build();
	}
	
	@POST
	@Path("addQuestion")
	@Produces("text/html")
	public Response addQuestion(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("type") String type,
			@FormParam("category") String category,
			@FormParam("question") String question,
			@FormParam("discussion") String discussion,
			@FormParam("references") String references,
			@FormParam("difficulty") String difficulty,
			@FormParam("attributes") List<String> attributes,
			@FormParam("answer1") String answer1,
			@FormParam("answer2") String answer2,
			@FormParam("answer3") String answer3,
			@FormParam("answer4") String answer4,
			@FormParam("answer5") String answer5,
			@FormParam("correct1") Boolean correct1,
			@FormParam("correct2") Boolean correct2,
			@FormParam("correct3") Boolean correct3,
			@FormParam("correct4") Boolean correct4,
			@FormParam("correct5") Boolean correct5
			) throws Exception, AuthenticationException {
		
		Question q = null;

		LOG.info("Type       --> {}", type);
		LOG.info("Category   --> {}", category);
		LOG.info("Question   --> {}", question);
		LOG.info("Discussion --> {}", discussion);
		LOG.info("References --> {}", references);
		LOG.info("Attributes --> {}", attributes);
		LOG.info("Answer1    --> {}, ()", answer1, correct1);
		LOG.info("Answer2    --> {}, ()", answer2, correct2);
		LOG.info("Answer3    --> {}, ()", answer3, correct3);
		LOG.info("Answer4    --> {}, ()", answer4, correct4);
		LOG.info("Answer5    --> {}, ()", answer5, correct5);
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		// Remove any nulls
		correct1 = (correct1 == null) ? new Boolean(false) : correct1;
		correct2 = (correct2 == null) ? new Boolean(false) : correct2;
		correct3 = (correct3 == null) ? new Boolean(false) : correct3;
		correct4 = (correct4 == null) ? new Boolean(false) : correct4;
		correct5 = (correct5 == null) ? new Boolean(false) : correct5;
		
		List<Answer> answers = new ArrayList<Answer>();
		if ( ! answer1.isEmpty()) answers.add(new Answer(answer1, correct1));
		if ( ! answer2.isEmpty()) answers.add(new Answer(answer2, correct2));
		if ( ! answer3.isEmpty()) answers.add(new Answer(answer3, correct3));
		if ( ! answer4.isEmpty()) answers.add(new Answer(answer4, correct4));
		if ( ! answer5.isEmpty()) answers.add(new Answer(answer5, correct5));
		
		// Reset the indexes to the current order
		int i = 1;
		for (Answer a : answers) {
			a.setIndex(i++);
		}
		
		if (category.toUpperCase().equals("SOP")) {
		    q = new Question(Type.BLANK, Category.SOP, attributes, 
					question, references, answers, discussion);
		} else {
			if (type.equals("fib")) {
			    q = new Question(Type.BLANK, Category.valueOf(category.toUpperCase()), attributes, 
						question, references, answers, discussion);
			} else {
			    q = new Question(Type.CHOICE, Category.valueOf(category.toUpperCase()), attributes, 
						question, references, answers, discussion);
			}
		}
		LOG.info("Question : {}", q);
		q.save();
		
		Flash.add(Flash.Code.SUCCESS, "Created new question with ID : " + q.getQuestionId());
		return new RedirectResponse(Pages.ADD_QUESTION_PAGE).build();
	}
	
	private String getUserCredentials(String email) {
		User user = User.getByEmail(email);
		if (user != null) {
			return user.getPassword();
		} else {
			return null;
		}
	}
	
	@GET
	@Path("attributes/{category}")
	@Produces("application/json")
	public Response attributes(@PathParam("category") String category) {
		List<String> attributes = Attribute.attributes(category);
		return Response.ok().entity(attributes).build();
	}
	
	private Writer renderFreemarker(String template, Object entity) throws IOException {
		Template temp = freemarker.getTemplate(template);

		Writer out = new StringWriter();
		try {
			temp.process(entity, out);
		} catch (TemplateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return out;
	}
}
