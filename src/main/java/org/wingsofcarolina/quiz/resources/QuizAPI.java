package org.wingsofcarolina.quiz.resources;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
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
import org.wingsofcarolina.quiz.domain.Recipe;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.Type;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.domain.presentation.Renderer;
import org.wingsofcarolina.quiz.responses.LoginResponse;
import org.wingsofcarolina.quiz.responses.RedirectResponse;
import org.wingsofcarolina.quiz.responses.ViewQuestionResponse;
import org.wingsofcarolina.quiz.scripting.Execute;

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
	private SimpleDateFormat dateFormatGmt;

	public QuizAPI(QuizConfiguration config) throws IOException {
		this.config = config;
		authUtils = new AuthUtils();
		objectMapper = new ObjectMapper();
		dataDir = config.getDataDirectory();
		questionDir = dataDir + "/questions";
		renderer = new Renderer(config);

		// Get the startup date/time format in GMT
		dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	@POST
	@Path("login")
	@Produces(MediaType.APPLICATION_JSON)
	public Response login(@FormParam("email") String email, @FormParam("password") String password,
			@FormParam("type") String type) throws Exception {

		if (email.isEmpty() || password.isEmpty()) {
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

		User user = User.getByEmail(email);
		LOG.info("Logged in  : {}", user);
		Slack.instance().sendMessage("Logged in  : " + user.getName() + " (" + user.getEmail() + ")");

		return new LoginResponse(authUtils.generateCookie(user)).build();
	}

	@GET
	@Path("logout")
	@Produces(MediaType.APPLICATION_JSON)
	public Response logout() throws Exception {
		NewCookie newCookie = new NewCookie("quiz.token", "", "/", "", "Quiz Login Token", 0, false);
		return new RedirectResponse(Pages.LOGIN_PAGE).cookie(newCookie).build();
	}

	@POST
	@Path("register")
	@Produces(MediaType.APPLICATION_JSON)
	public Response register(@CookieParam("quiz.token") Cookie cookie, @FormParam("action") String action,
			@FormParam("name") String name, @FormParam("email") String email, @FormParam("password") String password,
			@FormParam("passwordVerify") String passwordVerify, @FormParam("phone") String phone,
			@FormParam("member") String member) throws Exception, AuthenticationException {

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User requester = User.getWithClaims(claims);

		User user = null;

		if (action.equals("Register")) {
			if (email == null || password == null) {
				Flash.add(Flash.Code.ERROR, "No email or password provided, try again.");
				return new RedirectResponse(Pages.REGISTER_PAGE).build();
			} else if (!password.equals(passwordVerify)) {
				Flash.add(Flash.Code.ERROR, "Passwords do not match.");
				return new RedirectResponse(Pages.REGISTER_PAGE).build();
			}
	
			if (User.getByEmail(email) == null) {
				user = QuizResource.instance().addUser(name, email, password, Privilege.USER);
				user.save();
			} else {
				Flash.add(Flash.Code.ERROR, "User already exists.");
				return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(requester)).build();
			}
	
			// Generate user token
			LOG.info("Registered  : {}", user);
			Flash.add(Flash.Code.SUCCESS, "User with registered successfully.");
		}
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(requester)).build();
	}

	@POST
	@Path("changePassword")
	@Produces("text/html")
	public Response changePassword(@CookieParam("quiz.token") Cookie cookie, @FormParam("password") String password,
			@FormParam("passwordVerify") String passwordVerify)
			throws AuthenticationException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
		User user = User.getWithClaims(claims);

		if (password == null || passwordVerify == null) {
			Flash.add(Flash.Code.ERROR, "No password or verification password provided, try again.");
		} else if (!password.equals(passwordVerify)) {
			Flash.add(Flash.Code.ERROR, "Passwords do not match.");
		}

		// Actually change the password for the user
		LOG.info("Changing the password for '" + user.getEmail() + "'.");
		String hashedPw = HashUtils.generateStrongPasswordHash(password);
		user.setPassword(hashedPw);
		user.save();

		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@POST
	@Path("retrieve")
	@Produces("text/html")
	public Response retrieve(@CookieParam("quiz.token") Cookie cookie, @FormParam("quizId") long quizId,
			@FormParam("type") String type) throws AuthenticationException, IOException {

		String output = "";
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
		User user = User.getWithClaims(claims);

		Record record = Record.getByQuizId(quizId);
		if (record != null) {
			Quiz quiz = Quiz.quizFromRecord(record);
			if (type.equals("Key")) {
				output = renderer.render(Templates.KEY, quiz).toString();
				LOG.info("Quiz Key for quiz ID {} retrieved by {}", quizId, user.getName());
				Slack.instance().sendMessage("Quiz Key for quiz ID " + quizId + " retrieved by " + user.getName()
						+ " at " + dateFormatGmt.format(new Date()));
			} else {
				output = renderer.render(Templates.QUIZ, quiz).toString();
				LOG.info("Quiz Copy for quiz ID {} retrieved by {}", quizId, user.getName());
				Slack.instance().sendMessage("Quiz Copy for quiz ID " + quizId + " retrieved by " + user.getName()
						+ " at " + dateFormatGmt.format(new Date()));
			}
			return Response.ok().entity(output).build();
		}
		Flash.add(Flash.Code.ERROR, "Requested quiz not found.");
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@GET
	@Path("globalKey")
	@Produces("text/html")
	public Response globalKey(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException, IOException {

		String output = "";
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
		User user = User.getWithClaims(claims);

		Quiz quiz = new Quiz();
		List<Question> allQuestions = Question.getAllQuestions();
		
		// Fabricate a dummy quiz
		quiz.setQuizId(0L);
		quiz.addAll(allQuestions);
		quiz.setQuizName("All Questions");
		
		// Set the question numbers for the key
		int index = 1;
		for (Question question : allQuestions) {
			question.setIndex(index++);
		}
		
		output = renderer.render(Templates.KEY, quiz).toString();
		LOG.info("Global Database Quiz Key retrieved by {}", user.getName());
		Slack.instance().sendMessage("Global Database Quiz Key  retrieved by " + user.getName()
				+ " at " + dateFormatGmt.format(new Date()));
		return Response.ok().entity(output).build();
	}

	@GET
	@Path("backup")
	public Response backupDatabase(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException {
		int q_count = 0;
		int r_count = 0;

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Notify someone that a backup has been requested
		Slack.instance().sendMessage("Backup database requested at " + dateFormatGmt.format(new Date()));
		LOG.info("Backup of database requested at {}", dateFormatGmt.format(new Date()));

		// Make the directory if it doesn't exist
		new File(dataDir).mkdir();
		new File(questionDir).mkdir();

		List<Question> questions = Question.getAllQuestions();
		for (Question question : questions) {
			String name = questionDir + "/q-" + question.getQuestionId() + ".json";
			try {
				objectMapper.writeValue(new File(name), question);
				q_count++;
			} catch (IOException e) {
				LOG.info("IOException writing question {}", name, e);
			}
		}

		String name;
		List<Recipe> recipes = Recipe.getAllRecipes();
		for (Recipe recipe : recipes) {
			if (recipe.getAttribute() == null) {
				name = questionDir + "/r-" + recipe.getCategory() + ".json";
			} else {
				name = questionDir + "/r-" + recipe.getCategory() + "-" + recipe.getAttribute() + ".json";
			}
			try {
				objectMapper.writeValue(new File(name), recipe);
				r_count++;
			} catch (IOException e) {
				LOG.info("IOException writing recipe {}", name, e);
			}
		}

		LOG.info("The number of recipes saved : {}", r_count);
		LOG.info("The number of questions saved : {}", q_count);
		Flash.add(Flash.Code.SUCCESS, "Saved " + r_count + " recipes and " + q_count + " questions from the database.");
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@GET
	@Path("restore")
	public Response restoreDatabase(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException {
		int q_count = 0;
		int r_count = 0;

		Long maxID = 0L;

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Notify someone that a backup has been requested
		Slack.instance().sendMessage("Restore of all questions requested at " + dateFormatGmt.format(new Date()));
		LOG.info("Restore of all questions requested at {}", dateFormatGmt.format(new Date()));

		// First delete all questions and recipes (shudder)
		Recipe.drop();
		Question.drop();

		// Then pull in each file, one at a time, and create/save the question
		File folder = new File(questionDir);
		if (folder.isDirectory()) {
			// Restore the Recipes first
			File[] listOfFiles = folder.listFiles();
			for (File file : listOfFiles) {
				if (file.getName().startsWith(("r-"))) {
					try {
						Recipe recipe = objectMapper.readValue(file, Recipe.class);
						recipe.save();
					} catch (IOException e) {
						LOG.error("Failed reastoring recipe from file : {}", e.getMessage());
					}
					r_count++;
				}
			}

			// Then restore the questions
			listOfFiles = folder.listFiles();
			for (File file : listOfFiles) {
				if (file.getName().startsWith(("q-"))) {
					try {
						Question question = objectMapper.readValue(file, Question.class);
						if (question.getQuestionId() > maxID) {
							maxID = question.getQuestionId();
						}
						question.save();
					} catch (IOException e) {
						LOG.error("Failed reastoring question from file : {}", e.getMessage());
					}
					q_count++;
				}
			}
			LOG.info("Resetting maximum question ID in database to : {}", maxID);
			Persistence.instance().setID(Question.ID_KEY, maxID);
		} else {
			LOG.error("Directory {} not found during question database recovery.", questionDir);
		}

		LOG.info("The number of recipes restored : {}", r_count);
		LOG.info("The number of questions restored : {}", q_count);

		Flash.add(Flash.Code.SUCCESS,
				"Restored " + r_count + " recipes and " + q_count + " questions into the database.");
		Slack.instance()
				.sendMessage("Restored " + r_count + " recipes and " + q_count + " questions into the database.");
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@POST
	@Path("upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@CookieParam("quiz.token") Cookie cookie, 
			@FormDataParam("file") final InputStream fileInputStream,
			@FormDataParam("file") final FormDataContentDisposition contentDispositionHeader) throws IOException, AuthenticationException  {

		Long maxID = 0L;

//		// Validate that the user is permitted to perform this operation
//		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
//		User user = User.getWithClaims(claims);
		User user = User.getByEmail("dfrye@planez.co");

		// First delete all questions and recipes (shudder)
		Recipe.drop();
		Question.drop();
		
		// create a buffer to improve copy performance later.
		byte[] buffer = new byte[4096];

		ZipInputStream stream = new ZipInputStream(fileInputStream);

		// Iterate through each item in the stream. The get next
		// entry call will return a ZipEntry for each file in the
		// stream
		ZipEntry entry;
		while ((entry = stream.getNextEntry()) != null) {
			// Once we get the entry from the stream, the stream is
			// positioned read to read the raw data, and we keep
			// reading until read returns 0 or less.
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			int length;
			while ((length = stream.read(buffer)) != -1) {
			    result.write(buffer, 0, length);
			}
			
			try {
			if (entry.getName().startsWith("quizdata/r-")) {
				LOG.info("Deserializing recipe {}", entry.getName());
				Recipe recipe = objectMapper.readValue(result.toString("UTF-8"), Recipe.class);
				recipe.save();
			} else if (entry.getName().startsWith("quizdata/q-")) {
				LOG.info("Deserializing question {}", entry.getName());
				Question question = objectMapper.readValue(result.toString("UTF-8"), Question.class);
				if (question.getQuestionId() > maxID) {
					maxID = question.getQuestionId();
				}
				question.save();
			} else {
				LOG.error("Found an entry in the upload file that can't be deserialized : {}", entry.getName());
			}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		// we must always close the zip file.
		stream.close();
		
		LOG.info("Resetting maximum question ID in database to : {}", maxID);
		Persistence.instance().setID(Question.ID_KEY, maxID);
		
		return Response.status(200).entity("Zip file uploaded by " + user.getName() + "\n").build();

	}

	@POST
	@Path("addQuestion")
	@Produces(MediaType.TEXT_HTML)
	public Response addQuestion(@CookieParam("quiz.token") Cookie cookie, @FormParam("type") String typeName,
			@FormParam("category") String categoryName, @FormParam("question") String question,
			@FormParam("discussion") String discussion, @FormParam("references") String references,
			@FormParam("difficulty") String difficulty, @FormParam("attributes") List<String> attributes,
			@FormParam("answer") List<String> answers, @FormParam("correct") List<Integer> correct)
			throws Exception, AuthenticationException {

		Question newQuestion;

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		// Shortcut out if there are no answers ... that just ain't right
		if (empty(answers)) {
			Flash.add(Flash.Code.ERROR, "A question may not be saved with no answers.");
			return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
		}

		if (typeName.contentEquals("fib")) {
			newQuestion = createQuestion(cookie, typeName, categoryName, question, discussion, references, difficulty,
					attributes, answers, null);
		} else {
			if (correct == null || correct.size() == 0) {
				Flash.add(Flash.Code.ERROR, "A question may not be saved with no correct answer set.");
				return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
			}
			newQuestion = createQuestion(cookie, typeName, categoryName, question, discussion, references, difficulty,
					attributes, answers, correct.get(0));
		}

		return new ViewQuestionResponse(newQuestion.getQuestionId()).cookie(authUtils.generateCookie(user)).build();
	}

	@POST
	@Path("updateQuestion")
	@Produces("text/html")
	public Response updateQuestion(@CookieParam("quiz.token") Cookie cookie, @FormParam("questionId") Long questionId,
			@FormParam("type") String typeName, @FormParam("category") String categoryName,
			@FormParam("question") String question, @FormParam("discussion") String discussion,
			@FormParam("references") String references, @FormParam("difficulty") String difficulty,
			@FormParam("attributes") List<String> attributes, @FormParam("answer") List<String> answers,
			@FormParam("correct") List<Integer> correct, @FormParam("overwrite") Boolean overwrite)
			throws Exception, AuthenticationException {
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Shortcut out if there are no answers ... that just ain't right
		if (empty(answers)) {
			Flash.add(Flash.Code.ERROR, "A question may not be saved with no answers.");
			return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
		}

		Integer correctAnswer = null;

		boolean changed = false;
		if (overwrite == null)
			overwrite = false;

		// Add the difficulty to the attributes
		if (difficulty == null) {
			difficulty = "EASY";
		}
		attributes.add(difficulty);
		LOG.info("Attributes --> {}", attributes);

		// Get the existing question, and see if it has been deployed
		Question original = Question.getByQuestionId(questionId);
		// Pick up the correct answer
		if (!typeName.contentEquals("BLANK")) {
			correctAnswer = correct.get(0);
		}

		if (original != null) {
			if ((!original.getDeployed()) || overwrite == true) {
				// Update Attributes, detecting changes
				for (String att : attributes) {
					if (!original.hasAttribute(att)) {
						changed = true;
					}
				}
				if (attributes.size() != original.getAttributes().size()) {
					changed = true;
				}
				if (changed) {
					original.setAttributes(attributes);
				}

				// Update user-changeable details, detecting changes
				QuestionDetails details = new QuestionDetails(question, discussion, references, answers, correctAnswer);
				if (details.compareTo(original.getDetails()) != 0) {
					original.setDetails(details);
					changed = true;
				}

				if (changed) {
					LOG.info("Updated Question : {}", original.getQuestionId());
					original.save();
					Slack.instance().sendMessage("Updated Question : " + original.toString());
				} else {
					LOG.info("No changes detected for : {}", original.getQuestionId());
				}

				if (changed) {
					Flash.add(Flash.Code.SUCCESS, "Updated existing question with ID : " + original.getQuestionId());
				} else {
					Flash.add(Flash.Code.SUCCESS, "No changes made to question with ID : " + original.getQuestionId());
				}
			} else {
				Question q = createQuestion(cookie, typeName, categoryName, question, discussion, references,
						difficulty, attributes, answers, correctAnswer);
				original.setSupersededBy(q.getQuestionId());
				original.save();
				LOG.info("Superseded question " + original.getQuestionId() + " with " + q.getQuestionId());
				Flash.add(Flash.Code.SUCCESS,
						"Superseded question " + original.getQuestionId() + " with " + q.getQuestionId());
				Slack.instance().sendMessage("Superseded question " + original.getQuestionId() + " with "
						+ q.getQuestionId() + " at " + dateFormatGmt.format(new Date()));

				return new ViewQuestionResponse(q.getQuestionId()).cookie(authUtils.generateCookie(user)).build();
			}
		} else {
			Flash.add(Flash.Code.ERROR, "Question with ID " + questionId + " not found.");
		}
		return new ViewQuestionResponse(original.getQuestionId()).cookie(authUtils.generateCookie(user)).build();
	}

	// Determine of an array of answer strings is empty or not
	private boolean empty(List<String> answers) {
		if (answers != null || answers.size() > 0) {
			for (String answer : answers) {
				if ( ! answer.isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}
	
	private Question createQuestion(Cookie cookie, String typeName, String categoryName, String question,
			String discussion, String references, String difficulty, List<String> attributes, List<String> answers,
			Integer correct) throws Exception {

		Question newQuestion;

		LOG.info("Type       --> {}", typeName);
		LOG.info("Category   --> {}", categoryName);
		LOG.info("Question   --> {}", question);
		LOG.info("Discussion --> {}", discussion);
		LOG.info("References --> {}", references);
		LOG.info("Attributes --> {}", attributes);

		// Add the difficulty to the attributes
		attributes.add(difficulty);
		LOG.info("Attributes --> {}", attributes);

		Type type = null;
		Category category = null;
		if (categoryName.toUpperCase().startsWith("SOP")) {
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

		if (correct == null) {
			newQuestion = new Question(type, category, attributes,
					new QuestionDetails(question, discussion, references, answers));
		} else {
			newQuestion = new Question(type, category, attributes,
					new QuestionDetails(question, discussion, references, answers, correct));
		}
		Slack.instance().sendMessage("Created Question : " + newQuestion.getQuestionId());
		LOG.info("Created Question : {}", newQuestion.getQuestionId());
		newQuestion.save();

		Flash.add(Flash.Code.SUCCESS, "Created new question with ID : " + newQuestion.getQuestionId());
		return newQuestion;
	}

	@POST
	@Path("createExclusion")
	@Produces("text/html")
	public Response createExclusion(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("exclusions") List<Integer> exclusions)
			throws Exception, AuthenticationException {

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		for (Integer id : exclusions) {
			Long qid = Long.valueOf(id);
			Question question = Question.getByQuestionId(qid);
			if (question != null) {
				LOG.info("Updating exclusions for : {}", qid);
				question.setExclusions(exclusions);
				question.save();
			}
		}

		return new RedirectResponse(Pages.HOME_PAGE).build();
	}

	/**
	 * Delete question
	 * 
	 * Questions can only be deleted if they have not been either deployed or
	 * superseded. In either of those cases removing a question would break
	 * references either internally or (in a sense) externally.
	 * 
	 * @param cookie
	 * @param headers
	 * @param questionId
	 * @return
	 * @throws AuthenticationException
	 */
	@GET
	@Path("deleteQuestion/{id}")
	public Response deleteQuestion(@CookieParam("quiz.token") Cookie cookie, @Context HttpHeaders headers,
			@PathParam("id") Long questionId) throws AuthenticationException {
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		String referer = headers.getRequestHeader("referer").get(0);

		Question question = Question.getByQuestionId(questionId);
		if (question.isSuperseded() || question.getDeployed() == true) {
			Flash.add(Flash.Code.ERROR,
					"Question " + question.getQuestionId() + " is either superseded or deployed and can't be deleted.");
			return new RedirectResponse(referer).cookie(authUtils.generateCookie(user)).build();
		} else {
			// Actually perform the deletion
			question.delete();

			// Log the action
			LOG.info("Deleted question " + question.getQuestionId());
			Flash.add(Flash.Code.SUCCESS, "Deleted question " + question.getQuestionId());
			Slack.instance().sendMessage(
					"Deleted question " + question.getQuestionId() + " at " + dateFormatGmt.format(new Date()));
		}
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@POST
	@Path("updateRecipe")
	@Produces(MediaType.TEXT_HTML)
	public Response updateRecipe(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("category") String category,
			@FormParam("attribute") String attribute,
			@FormParam("script") String script
			) throws Exception, AuthenticationException {
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Retrieve the recipe
		Recipe recipe;
		if (attribute == null) {
			recipe = Recipe.getRecipeByCategory(Category.valueOf(category.toUpperCase()));
		} else {
			recipe = Recipe.getRecipeByCategoryAndAttribute(Category.valueOf(category.toUpperCase()),
				attribute.toUpperCase());
		}

		// Create a new recipe, if needed
		if (recipe == null) {
			recipe = new Recipe();
			recipe.setCategory(Category.valueOf(category.toUpperCase()));
			if (attribute != null) {
				recipe.setAttribute(attribute);
			}
		}
		recipe.setScript(script);
		
		// Save the recipe
		recipe.save();

		if (attribute != null) {
			Flash.add(Flash.Code.SUCCESS, "Recipe type " + recipe.getCategory() + " updated.");
		} else {
			Flash.add(Flash.Code.SUCCESS, "Recipe type " + recipe.getCategory() + "/" + attribute + " updated.");
		}

		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@GET
	@Path("recipe/{category}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRecipe(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("category") String category,
			@QueryParam("attribute") String attribute)
			throws Exception, AuthenticationException {

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Retrieve the recipe
		Recipe recipe;
		if (attribute == null) {
			recipe = Recipe.getRecipeByCategory(Category.valueOf(category.toUpperCase()));
		} else {
			recipe = Recipe.getRecipeByCategoryAndAttribute(Category.valueOf(category.toUpperCase()),
				attribute.toUpperCase());
		}
		return Response.ok().entity(recipe).cookie(authUtils.generateCookie(user)).build();

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
		for (String a : difficulties) {
			if (question.hasAttribute(a)) {
				attributes.add(new AttributeResponse(a, true));
			} else {
				attributes.add(new AttributeResponse(a, false));
			}
		}
		return Response.ok().entity(attributes).build();
	}

	@GET
	@Path("question/{id}")
	@Produces("application/json")
	public Response question(@PathParam("id") Long id) {
		Question question = Question.getByQuestionId(id);
		if (question != null) {
			return Response.ok().entity(question).build();
		} else {
			return Response.status(404).build();
		}
	}

	@GET
	@Path("question/attributes/{id}")
	@Produces("application/json")
	public Response questionAttributes(@PathParam("id") Long id) {
		Question question = Question.getByQuestionId(id);
		List<String> categoryAttributes = Attribute.attributes(question.getCategory().name());
		List<AttributeResponse> attributes = new ArrayList<AttributeResponse>();
		for (String att : categoryAttributes) {
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
	@Produces(MediaType.APPLICATION_JSON)
	public Response attributes(@PathParam("category") String category, @QueryParam("attribute") String attribute) {
		List<String> attributes = Attribute.attributes(category);
		return Response.ok().entity(attributes).build();
	}

	@POST
	@Path("script")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response runScript(Map<String, String> request) {
		String result;
		String status = "success";
		String category = request.get("category");
		String script = request.get("script");
		Map<String, String> response = new HashMap<String, String>();
		
		QuizContext context= new QuizContext(new Quiz(category), config);
		context.setTestRun(true);
		Execute execute = new Execute(context);
		try {
			result = execute.run(script);
		} catch (Exception mpe) {
			result = mpe.getLocalizedMessage();
			status = "failure";
		}

		// Generate output report (including HTML, ugh)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        ps.println("<h3>Script Output : </h3>");
        ps.println(result);
        ps.println("<h3>Questions : </h3><ul>");
        List<Question> questions = context.getQuiz().getQuestions();
        for (Question question : questions) {
        	ps.println("<li><a href=\"/question/" + question.getQuestionId() + "\">" + question.getQuestionId() + "</a> : " + trim(question.getQuestion()));
        }
        ps.println("</ul>");
        ps.println("Total number of questions : " + questions.size());
        ps.close();
		
		response.put("status", status);
		response.put("result", baos.toString());
		
		return Response.ok().entity(response).build();
	}
	
    private String trim(String line) {
    	String result = line;
    	int pos = line.indexOf('\n');
    	if (pos != -1) {
    		result = line.substring(0, pos);
    	}
    	return result;
    }
    
	@GET
	@Path("download")
	@Produces(MediaType.TEXT_PLAIN)
	public Response downloadBackup(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
			User.getWithClaims(claims);

			String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());

			// Notify someone that a backup has been requested
			Slack.instance().sendMessage("Download of all questions requested at " + dateFormatGmt.format(new Date()));
			LOG.info("Download of all questions requested at {}", dateFormatGmt.format(new Date()));

			StreamingOutput streamingOutput = outputStream -> {
				String name;

				ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));

				// Iterate over all recipes creating the zip output file
				List<Recipe> recipes = Recipe.getAllRecipes();
				for (Recipe recipe : recipes) {
					if (recipe.getAttribute() == null) {
						name = "quizdata/r-" + recipe.getCategory() + ".json";
					} else {
						name = "quizdata/r-" + recipe.getCategory() + "-" + recipe.getAttribute() + ".json";
					}
					ZipEntry zipEntry = new ZipEntry(name);
					zipOut.putNextEntry(zipEntry);
					try {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						objectMapper.writeValue(bos, recipe);
						byte[] ba = bos.toByteArray();
						zipOut.write(ba, 0, ba.length);
						zipOut.flush();
					} catch (IOException e) {
						LOG.info("IOException writing recipe {}", name, e);
					}
				}

				// Iterate over all questions creating the zip output file
				List<Question> questions = Question.getAllQuestions();
				for (Question question : questions) {
					name = "quizdata/q-" + question.getQuestionId() + ".json";
					ZipEntry zipEntry = new ZipEntry(name);
					zipOut.putNextEntry(zipEntry);
					try {
						ByteArrayOutputStream bos = new ByteArrayOutputStream();
						objectMapper.writeValue(bos, question);
						byte[] ba = bos.toByteArray();
						zipOut.write(ba, 0, ba.length);
						zipOut.flush();
					} catch (IOException e) {
						LOG.info("IOException writing question {}", name, e);
					}
				}
				zipOut.close();
				outputStream.flush();
				outputStream.close();
			};

			return Response.ok(streamingOutput).type(MediaType.TEXT_PLAIN)
					.header("Content-Disposition", "attachment; filename=\"quiz-" + now + ".zip\"").build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
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
