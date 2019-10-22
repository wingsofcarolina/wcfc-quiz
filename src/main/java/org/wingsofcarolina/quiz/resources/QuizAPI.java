package org.wingsofcarolina.quiz.resources;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
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
import org.wingsofcarolina.quiz.domain.Attribute;
import org.wingsofcarolina.quiz.domain.AutoIncrement;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
	public Response register(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("action") String action,
			@FormParam("name") String name,
			@FormParam("email") String email,
			@FormParam("password") String password,
			@FormParam("passwordVerify") String passwordVerify,
			@FormParam("phone") String phone,
			@FormParam("member") String member)
			throws Exception, AuthenticationException {
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User requester = User.getWithClaims(claims);

		User user = null;

		if (email == null || password == null) {
			Flash.add(Flash.Code.ERROR, "No email or password provided, try again.");
			return new RedirectResponse(Pages.REGISTER_PAGE).build();
		} else if (! password.equals(passwordVerify)) {
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
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(requester)).build();		
	}

	@POST
	@Path("changePassword")
	@Produces("text/html")
	public Response changePassword(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("password") String password,
			@FormParam("passwordVerify") String passwordVerify
			) throws AuthenticationException, IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
		User user = User.getWithClaims(claims);

		if (password == null || passwordVerify == null) {
			Flash.add(Flash.Code.ERROR, "No password or verification password provided, try again.");
		} else if (! password.equals(passwordVerify)) {
			Flash.add(Flash.Code.ERROR, "Passwords do not match.");
		}

		// Actually change the password for the user
		LOG.info("Changing the password for '" + user.getEmail() +"'.");
		String hashedPw = HashUtils.generateStrongPasswordHash(password);
		user.setPassword(hashedPw);
		user.save();
		
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
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
		
		Record record = Record.getByQuizId(quizId);
		if (record != null) {
			Quiz quiz = Quiz.quizFromRecord(record);
			if (type.equals("Key")) {
				output = renderer.render(Templates.KEY, quiz).toString();
				LOG.info("Quiz Key for quiz ID {} retrieved by {}", quizId, user.getName());
				Slack.instance().sendMessage("Quiz Key for quiz ID " + quizId + " retrieved by " + user.getName() + " at " + dateFormatGmt.format(new Date()));
			} else {
				output = renderer.render(Templates.QUIZ, quiz).toString();
				LOG.info("Quiz Copy for quiz ID {} retrieved by {}", quizId, user.getName());
				Slack.instance().sendMessage("Quiz Copy for quiz ID " + quizId + " retrieved by " + user.getName() + " at " + dateFormatGmt.format(new Date()));
			}
			return Response.ok().entity(output).build();
		}
		Flash.add(Flash.Code.ERROR, "Requested quiz not found.");
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@GET
	@Path("backupQuestions")
	public Response backupQuestions(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException {
		int count = 0;
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		// Notify someone that a backup has been requested
		Slack.instance().sendMessage("Backup of all questions requested at " + dateFormatGmt.format(new Date()));
		LOG.info("Backup of all questions requested at {}", dateFormatGmt.format(new Date()));

		// Make the directory if it doesn't exist
		new File(dataDir).mkdir();
		new File(questionDir).mkdir();
		
		List<Question> questions = Question.getAllQuestions();
		for (Question question : questions) {
			String name = questionDir + "/" + question.getQuestionId() + ".json";
			try {
				objectMapper.writeValue(new File(name), question);
				count++;
			} catch (IOException e) {
				LOG.info("IOException writing question {}", name, e);
			}
		}
		
		LOG.info("The number of questions saved : {}", count);
		Flash.add(Flash.Code.SUCCESS, "Saved " + count + " questions from the database.");
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@GET
	@Path("restoreQuestions")
	public Response restoreQuestions(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException {
		Long maxID = 0L;
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Notify someone that a backup has been requested
		Slack.instance().sendMessage("Restore of all questions requested at " + dateFormatGmt.format(new Date()));
		LOG.info("Restore of all questions requested at {}", dateFormatGmt.format(new Date()));

		// First delete all questions (shudder)
		Question.drop();

		// Then pull in each file, one at a time, and create/save the question
		int count = 0;
		File folder = new File(questionDir);
		if (folder.isDirectory()) {
			File[] listOfFiles = folder.listFiles();
			for (File file : listOfFiles) {
				try {
					Question question = objectMapper.readValue(file, Question.class);
					if (question.getQuestionId() > maxID) {
						maxID = question.getQuestionId();
					}
					question.save();
				} catch (IOException e) {
					LOG.error("Failed reastoring question from file : {}", e.getMessage());
				}
				count++;
			}
			LOG.info("Resetting maximum question ID in database to : {}", maxID);
			AutoIncrement inc = Persistence.instance().setID(Question.ID_KEY, maxID);
		} else {
			LOG.error("Directory {} not found during question database recovery.", questionDir);
		}

		LOG.info("The number of questions restored : {}", count);

		Flash.add(Flash.Code.SUCCESS, "Restored " + count + " questions into the database.");
		Slack.instance().sendMessage("Restored " + count + " questions into the database.");
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
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
			@FormParam("answer") List<String> answers,
			@FormParam("correct") List<Integer> correct
			) throws Exception, AuthenticationException {

		Question newQuestion;

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		if (typeName.contentEquals("fib")) {
			newQuestion = createQuestion(cookie, typeName, categoryName, question, discussion, references, difficulty,
					attributes, answers, null);
		} else {
			newQuestion = createQuestion(cookie, typeName, categoryName, question, discussion, references, difficulty,
				attributes, answers, correct.get(0));
		}
		
		return new ViewQuestionResponse(newQuestion.getQuestionId()).cookie(authUtils.generateCookie(user)).build();
	}
	
	@POST
	@Path("updateQuestion")
	@Produces("text/html")
	public Response updateQuestion(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("questionId") Long questionId,
			@FormParam("type") String typeName,
			@FormParam("category") String categoryName,
			@FormParam("question") String question,
			@FormParam("discussion") String discussion,
			@FormParam("references") String references,
			@FormParam("difficulty") String difficulty,
			@FormParam("attributes") List<String> attributes,
			@FormParam("answer") List<String> answers,
			@FormParam("correct") List<Integer> correct,
			@FormParam("overwrite") Boolean overwrite
			) throws Exception, AuthenticationException {
		
		boolean changed = false;
		if (overwrite == null) overwrite = false;
		
		// Add the difficulty to the attributes
		if (difficulty == null) {
			difficulty = "EASY";
		}
		attributes.add(difficulty);
		LOG.info("Attributes --> {}", attributes);
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		// Get the existing question, and see if it has been deployed
		Question original = Question.getByQuestionId(questionId);
		// Pick up the correct answer
		Integer correctAnswer = correct.get(0);

		if (original != null) {
			if ( (! original.getDeployed()) || overwrite == true) {
				// Update Attributes, detecting changes
				for (String att : attributes) {
					if ( ! original.hasAttribute(att)) { changed = true; }
				}
				if (attributes.size() != original.getAttributes().size()) { changed = true; }
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
				Question q = createQuestion(cookie, typeName, categoryName, question, discussion, references, difficulty, attributes, answers, correctAnswer);
				original.setSupercededBy(q.getQuestionId());
				original.save();
				LOG.info("Superseded question " + original.getQuestionId() + " with " +  q.getQuestionId());
				Flash.add(Flash.Code.SUCCESS, "Superseded question " + original.getQuestionId() + " with " + q.getQuestionId());
				Slack.instance().sendMessage("Superseded question " + original.getQuestionId() + " with " + q.getQuestionId() + " at " + dateFormatGmt.format(new Date()));
				
				return new ViewQuestionResponse(q.getQuestionId()).cookie(authUtils.generateCookie(user)).build();
			}
		} else {
			Flash.add(Flash.Code.ERROR, "Question with ID " + questionId + " not found.");			
		}
		return new ViewQuestionResponse(original.getQuestionId()).cookie(authUtils.generateCookie(user)).build();
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
			newQuestion = new Question(type, category, attributes, new QuestionDetails(question, discussion, references, answers));
		} else {
			newQuestion = new Question(type, category, attributes, new QuestionDetails(question, discussion, references, answers, correct));
		}
		Slack.instance().sendMessage("Created Question : " + newQuestion.getQuestionId());
		LOG.info("Created Question : {}", newQuestion.getQuestionId());
		newQuestion.save();
		
		Flash.add(Flash.Code.SUCCESS, "Created new question with ID : " + newQuestion.getQuestionId());
		return newQuestion;
	}
	
	/**
	 * Delete question
	 * 
	 * Questions can only be deleted if they have not been either deployed or superseded. In
	 * either of those cases removing a question would break references either internally or
	 * (in a sense) externally.
	 * 
	 * @param cookie
	 * @param headers
	 * @param questionId
	 * @return
	 * @throws AuthenticationException
	 */
	@GET
	@Path("deleteQuestion/{id}")
	public Response deleteQuestion(@CookieParam("quiz.token") Cookie cookie,
			@Context HttpHeaders headers,
			@PathParam("id") Long questionId) throws AuthenticationException {
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);
		
		String referer = headers.getRequestHeader("referer").get(0);

		Question question = Question.getByQuestionId(questionId);
		if (question.isSuperceded() || question.getDeployed() == true) {
			Flash.add(Flash.Code.ERROR, "Question " + question.getQuestionId() + " is either superseded or deployed and can't be deleted.");
			return new RedirectResponse(referer).cookie(authUtils.generateCookie(user)).build();
		} else {
			// Actually perform the deletion
			question.delete();
			
			// Log the action
			LOG.info("Deleted question " + question.getQuestionId());
			Flash.add(Flash.Code.SUCCESS, "Deleted question " + question.getQuestionId());
			Slack.instance().sendMessage("Deleted question " + question.getQuestionId() + " at " + dateFormatGmt.format(new Date()));
		}
		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
	}

	@POST
	@Path("updateRecipe")
	@Produces("text/html")
	public Response updateRecipe(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("recipe") String recipe
			) throws Exception, AuthenticationException {
		
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Create the new recipe object
		Recipe newRecipe = objectMapper.readValue(recipe, Recipe.class);

		// Get rid of the old recipe
		Recipe original = Recipe.getRecipeByCategoryAndAttribute(newRecipe.getCategory(), newRecipe.getAttribute().toString());
		original.delete();
		
		// Replace it with the new recipe
		newRecipe.save();
		
		Flash.add(Flash.Code.SUCCESS, "Recipe type " + newRecipe.getCategory() + " updated.");			

		return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
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
	public Response attributes(@PathParam("category") String category,
			@QueryParam("attribute") String attribute) {
		List<String> attributes = Attribute.attributes(category);
		return Response.ok().entity(attributes).build();
	}

	@GET
	@Path("download")
	@Produces(MediaType.TEXT_PLAIN)
	public Response downloadQuestions(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException {
		
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
			User user = User.getWithClaims(claims);
	
			String now = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
	
			// Notify someone that a backup has been requested
			Slack.instance().sendMessage("Download of all questions requested at " + dateFormatGmt.format(new Date()));
			LOG.info("Download of all questions requested at {}", dateFormatGmt.format(new Date()));
			
		    StreamingOutput streamingOutput = outputStream -> {
		        ZipOutputStream zipOut = new ZipOutputStream(new BufferedOutputStream(outputStream));
	
		        // Iterate over all questions creating the zip output file
				List<Question> questions = Question.getAllQuestions();
				for (Question question : questions) {
					String name = "quiz-" + now + "/" + question.getQuestionId() + ".json";
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
	
		    return Response.ok(streamingOutput)
		            .type(MediaType.TEXT_PLAIN)
		            .header("Content-Disposition","attachment; filename=\"quiz-" + now + ".zip\"")
		            .build();
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
