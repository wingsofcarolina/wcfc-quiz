package org.wingsofcarolina.quiz.resources;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
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
import org.wingsofcarolina.quiz.domain.QuestionDetails;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.Type;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.responses.LoginResponse;
import org.wingsofcarolina.quiz.responses.RedirectResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

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
	private ObjectMapper objectMapper;
	private String dataDir;
	private String questionDir;
	private Renderer renderer;

	public QuizAPI(QuizConfiguration config) throws IOException {
		this.config = config;
		authUtils = new AuthUtils();
		objectMapper = new ObjectMapper();
		dataDir = config.getDataDirectory();
		questionDir = dataDir + "/questions";
		renderer = new Renderer(config);
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
				output = renderer.render(Templates.KEY, quiz).toString();
			} else {
				output = renderer.render(Templates.QUIZ, quiz).toString();
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
			@FormParam("type") String typeName,
			@FormParam("category") String categoryName,
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
		
		LOG.info("Type       --> {}", typeName);
		LOG.info("Category   --> {}", categoryName);
		LOG.info("Question   --> {}", question);
		LOG.info("Discussion --> {}", discussion);
		LOG.info("References --> {}", references);
		LOG.info("Attributes --> {}", attributes);
		

		// Add the difficulty to the attributes
		attributes.add(difficulty);
		LOG.info("Attributes --> {}", attributes);
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		Type type = null;
		Category category = null;
		if (categoryName.toUpperCase().equals("SOP")) {
		    type = Type.BLANK;
		    category = Category.SOP;
		} else {
			if (typeName.equals("fib")) {
			    type = Type.BLANK;
			    category = Category.valueOf(categoryName.toUpperCase());
			} else {
			    type = Type.CHOICE;
			    category = Category.valueOf(categoryName.toUpperCase());
			}
		}
		
		Question q = new Question(type, category, attributes, new QuestionDetails(question, discussion, references, answer1,
				answer2, answer3, answer4, answer5, correct1, correct2, correct3, correct4, correct5));
		LOG.info("Created Question : {}", q);
		q.save();
		
		Flash.add(Flash.Code.SUCCESS, "Created new question with ID : " + q.getQuestionId());
		return new RedirectResponse(Pages.HOME_PAGE).build();
	}
	
	@POST
	@Path("updateQuestion")
	@Produces("text/html")
	public Response updateQuestion(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("questionId") Long questionId,
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
		
		boolean changed = false;
		LOG.info("QuestionId --> {}", questionId);

		// Add the difficulty to the attributes
		attributes.add(difficulty);
		LOG.info("Attributes --> {}", attributes);
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		// Get the existing question, and see if it has been deployed
		Question original = Question.getByQuestionId(questionId);
	
		if (original != null) {
			// Update Attributes, detecting changes
			for (String att : attributes) {
				if ( ! original.hasAttribute(att)) { changed = true; }
			}
			if (attributes.size() != original.getAttributes().size()) { changed = true; }
			if (changed) {
				original.setAttributes(attributes);
			}
			
			// Update user-changeable details, detecting changes
			QuestionDetails details = new QuestionDetails(question, discussion, references, answer1,
					answer2, answer3, answer4, answer5, correct1, correct2, correct3, correct4, correct5);
			if (details.update(original)) { changed = true; }
			
			if (changed) {
				LOG.info("Updated Question : {}", original.getQuestionId());
				original.save();
			} else {
				LOG.info("No changes detected for : {}", original.getQuestionId());
			}
			
			if (changed) {
				Flash.add(Flash.Code.SUCCESS, "Updated existing question with ID : " + original.getQuestionId());
			} else {
				Flash.add(Flash.Code.SUCCESS, "No changes made to question with ID : " + original.getQuestionId());
			}
		} else {
			Flash.add(Flash.Code.SUCCESS, "Question with ID " + questionId + " not found.");			
		}
		return new RedirectResponse(Pages.HOME_PAGE).build();
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
	@Path("difficulty")
	@Produces("application/json")
	public Response difficulty() {
		List<String> attributes = Attribute.attributes("difficulty");
		return Response.ok().entity(attributes).build();
	}
	
	@GET
	@Path("question/difficulty/{id}")
	@Produces("application/json")
	public Response questionDifficulty(@PathParam("id") Long id) {
		Question question = Question.getByQuestionId(id);
		List<String> difficulties = Attribute.attributes("difficulty");
		List<AttributeResponse> attributes = new ArrayList<AttributeResponse>();
		for (String a: difficulties) {
			if (question.hasAttribute(a)) {
				attributes.add(new AttributeResponse(a, true));
			} else {
				attributes.add(new AttributeResponse(a, false));
			}
		}
		return Response.ok().entity(attributes).build();
	}
	
	@GET
	@Path("question/attributes/{id}")
	@Produces("application/json")
	public Response questionAttributes(@PathParam("id") Long id) {
		Question question = Question.getByQuestionId(id);
		List<String> categoryAttributes = Attribute.attributes(question.getCategory().name());
		List<AttributeResponse> attributes = new ArrayList<AttributeResponse>();
		for (String att: categoryAttributes) {
			if (question.hasAttribute(att)) {
				attributes.add(new AttributeResponse(att, true));
			} else {
				attributes.add(new AttributeResponse(att, false));
			}
		}
		return Response.ok().entity(attributes).build();
	}
	
	@GET
	@Path("attributes/{category}")
	@Produces("application/json")
	public Response attributes(@PathParam("category") String category) {
		List<String> attributes = Attribute.attributes(category);
		return Response.ok().entity(attributes).build();
	}

	
	class AttributeResponse {
		private String label;
		public boolean checked;
		
		public AttributeResponse(String label, boolean checked) {
			this.label = label;
			this.checked = checked;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public boolean isChecked() {
			return checked;
		}

		public void setChecked(boolean checked) {
			this.checked = checked;
		}
	}
}
