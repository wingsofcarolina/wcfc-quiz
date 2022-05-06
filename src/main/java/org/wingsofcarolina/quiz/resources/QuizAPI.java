package org.wingsofcarolina.quiz.resources;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
import org.wingsofcarolina.quiz.common.QuizBuildException;
import org.wingsofcarolina.quiz.common.Templates;
import org.wingsofcarolina.quiz.domain.Answer;
import org.wingsofcarolina.quiz.domain.Attribute;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.ExclusionGroup;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.QuestionDetails;
import org.wingsofcarolina.quiz.domain.Recipe;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.Type;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;
import org.wingsofcarolina.quiz.domain.presentation.PDFGenerator;
import org.wingsofcarolina.quiz.domain.presentation.QuestionWrapper;
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
	private QuizConfiguration config;
	private AuthUtils authUtils;
	private ObjectMapper objectMapper;
	private String dataDir;
	private String questionDir;
	private String imageDir;
	private Renderer renderer;
	private SimpleDateFormat dateFormatGmt;

	public QuizAPI(QuizConfiguration config) throws IOException {
		this.config = config;
		authUtils = new AuthUtils();
		objectMapper = new ObjectMapper();
		dataDir = config.getDataDirectory();
		config.getAssetDirectory();
		imageDir = config.getImageDirectory();
		questionDir = dataDir + "/questions";
		renderer = new Renderer(config);

		// Get the startup date/time format in GMT
		dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	@GET
	@Path("deployed/{questionId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response explore(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("questionId") Long questionId) throws Exception, AuthenticationException {
		return Response.ok().entity(Record.isQuestionIdDeployed(questionId)).build();
	}
	
//	@GET
//	@Path("explore")
//	@Produces(MediaType.APPLICATION_JSON)
//	public Response explore(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
//		Set<Question> remove = new HashSet<Question>();
//
//		List<Question> result = Question.getWithAny(Arrays.asList("C152", "SYSTEMS", "STUDENT"));
//		Set<Long> active = Record.getActiveIds();
//		List<Question> superseded = Question.getSuperseded();
//		for (Question question : superseded) {
//			System.out.print("Question " + question.getQuestionId());
//			remove.addAll(followChain(question, active));
//		}
//		
//		for (Question question : remove) {
//			System.out.println("Removing : " + question.getQuestionId());
//			question.delete();
//		}
//		
//		return Response.ok().entity(superseded).build();
//	}
//	
//	private Set<Question> followChain(Question question, Set<Long> active) {
//		// Question 2002 active, superseded by 2360 superseded by 2361 active, superseded by 2362
//		Set<Question> remove = new HashSet<Question>();
//		Question previous = question;
//		do {
//			if (active.contains(question.getQuestionId())) {
//				previous = question;
//				System.out.print(" (active), superseded by " + question.getSupersededBy());
//			} else {
//				System.out.print(" superseded by " + question.getSupersededBy());
//				System.out.println("");
//				System.out.print("    " + previous.getQuestionId() + " now superseded by " + question.getSupersededBy());
//				
//				previous.setSupersededBy(question.getSupersededBy());
//				previous.save();
//				remove.add(question);
//			}
//			question = Question.getByQuestionId(question.getSupersededBy());
//			if (question == null) break;
//		} while (question.getSupersededBy() != -1);
//		System.out.println();
//		return remove;
//	}

//	@GET
//	@Path("migrateRecipes")
//	@Produces(MediaType.APPLICATION_JSON)
//	public Response allRecipes(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
//		List<Recipe> recipes = Recipe.getAllRecipes();
//		
//		migrateRecipes(recipes);
//		
//		return Response.ok().entity(recipes).build();
//	}
//	
//	private void migrateRecipes(List<Recipe> recipes) {
//		for (Recipe r : recipes) {
//			r.init();
//			String attribute = r.getAttribute();
//			if (attribute == null || attribute.isEmpty()) {
//				if (r.getCategory().toString().equals("FAR")) {
//					r.setName("FAR,Pre-solo");
//				} else {
//					r.setName(r.getCategory().toString());
//				}
//			} else {
//				r.setName(r.getCategory() + "-" + attribute);
//			}
//			r.setAttribute(null);
//			r.setCategory(null);
//			r.setSections(null);
//			r.save();
//		}
//	}
	
	@GET
	@Path("alias")
	@Produces(MediaType.APPLICATION_JSON)
	public Response alias(@CookieParam("quiz.token") Cookie cookie) {
		 Map<Long, String> map = Stream.of(new Object[][] { 
		     { 1000L, "FAR" }, 
		     { 1001L, "SOP_STUDENT" }, 
		     { 1002L, "SOP_PILOT" }, 
		     { 1003L, "SOP_INSTRUCTOR" }, 
		     { 1004L, "C152" }, 
		     { 1005L, "PA28" }, 
		     { 1006L, "C172" }, 
		     { 1007L, "M20J" }, 
		 }).collect(Collectors.toMap(data -> (Long) data[0], data -> (String) data[1]));
		 
		 Map<Long, String> recipeCatalog = new HashMap<Long, String>();
		
		List<Recipe> recipes = Recipe.getAllRecipes();
		
		for (Recipe recipe : recipes) {
			String alias = map.get(recipe.getRecipeId());
			if (alias != null) {
				recipe.setAlias(alias);
				recipe.save();
			} else {
				System.out.println("Woah! Something ain't right. Failed Alias lookup!!!");
			}
		}
		
		return Response.ok().entity(recipeCatalog).build();
	}

	@GET
	@Path("recipes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response recipes(@CookieParam("quiz.token") Cookie cookie) {
//		Map<String, String> recipeCatalog = new HashMap<String, String>();
		
		List<Recipe> recipes = Recipe.getAllRecipes();
		
//		for (Recipe r : recipes) {
//			recipeCatalog.put(r.getAlias(), r.getName());
//		}
		
		return Response.ok().entity(recipes).build();
	}

	@GET
	@Path("baseline")
	@Produces(MediaType.APPLICATION_JSON)
	public Response baseline() {
		// First remove all questions that have been superseded
		List<Question> questions = Question.getAllQuestions();
		for (Question question : questions) {
			if (question.isSuperseded()) {
				LOG.info("Deleting : {}", question.getQuestionId());
				question.delete();
			} else {
				List<String> attributes = question.getAttributes();
				Collections.sort(attributes);
				question.setAttributes(attributes);
				question.save();
			}
		}
		
		// Then remove ALL records
		List<Record> records = Record.getAllRecords();
		for (Record record : records) {
			record.delete();
		}
		
		return Response.ok().build();
	}
	
	@GET
	@Path("cleanup")
	@Produces(MediaType.APPLICATION_JSON)
	public Response migrate() {
		List<Question> questions = Question.getAllQuestions();
		for (Question question : questions) {
			boolean changed = false;
			Long id = question.getQuestionId();
			String newString = scrub(id, question.getQuestion());
			if (newString != null) {
				question.setQuestion(newString);
				changed = true;
			}
			newString = scrub(id, question.getDiscussion());
			if (newString != null) {
				question.setDiscussion(newString);
				changed = true;
			}
			newString = scrub(id, question.getReferences());
			if (newString != null) {
				question.setReferences(newString);
				changed = true;
			}
			List<Answer> answers = question.getAnswers();
			if (answers != null) {
				for (Answer answer : answers) {
					newString = scrub(id, answer.getAnswer());
					if (newString != null) {
						answer.setAnswer(newString);
						changed = true;
					}
				}
			}
			if (changed) {
				question.save();
			}
		}
		return Response.ok().build();
	}

	private String scrub(Long id, String text) {
		boolean found = false;
		char[] target = text.toCharArray();
		for (int i = 0; i < target.length; i++) {
			if (target[i] > 8000) {
				int value = target[i];
				System.out.println("======> " + id + " <> " + target[i] + " : " + value);
				switch (value) {
					case 8217 : target[i] = '\''; break;
					case 8220 : 
					case 8221 : target[i] = '\"'; break;
					case 8209 : 
					case 8211 : target[i] = '-'; break;
					case 8226 : target[i] = '*'; break;
				}
				found = true;
			}
		}
		
		if (found) { 
			return new String(target);
		} else {
			return null;
		}
	}

//	@GET
//	@Path("migrateQuestions")
//	@Produces(MediaType.APPLICATION_JSON)
//	public Response allQuestions() {
//		List<Question> list = Question.getAllQuestions();
//
//		migrateQuestions(list);
//
//		return Response.ok().entity(list).build();
//	}
//	
//	void migrateQuestions(List<Question> list) {
//		for (Question q : list) {
//			migrateQuestion(q);
//			q.save();
//		}
//	}
//	
//	private void migrateQuestion(Question q) {
//		List<String> attributes = q.getAttributes();
//		
//		boolean isSOP = false;
//		for (String att : attributes) {
//			if (att != null) {
//				if (att.startsWith("SOP_")) {
//				 isSOP = true;
//				 break;
//				}
//			} else {
//				LOG.info("Null attribute list for : " + q.getQuestionId());
//			}
//		}
//		
//		if (isSOP) {
//			q.addAttribute("SOP");
//			q.removeAttribute("FAR");
//		}
//		
//		Category category = q.getCategory();
//		if (category == Category.FAR) {
//			q.addAttribute(Attribute.FAR);
//			q.setCategory(Category.REGULATIONS);
//		} else if (category == Category.SOP) {
//			q.addAttribute(Attribute.SOP);
//			q.setCategory(Category.REGULATIONS);
//		} else if (category == Category.C152) {
//			q.addAttribute(Attribute.C152);
//			q.setCategory(Category.AIRCRAFT);
//		} else if (category == Category.PA28) {
//			q.addAttribute(Attribute.PA28);
//			q.setCategory(Category.AIRCRAFT);
//		} else if (category == Category.C172) {
//			q.addAttribute(Attribute.C172);
//			q.setCategory(Category.AIRCRAFT);
//		} else if (category == Category.M20J) {
//			q.addAttribute(Attribute.M20J);
//			q.setCategory(Category.AIRCRAFT);
//		}
//	}
	
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
			@FormParam("type") String type) throws AuthenticationException, IOException, QuizBuildException, URISyntaxException {

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
				return Response.ok().entity(output).build();
			} else {
				//output = renderer.render(Templates.QUIZ, quiz).toString();
				LOG.info("Quiz Copy for quiz ID {} retrieved by {}", quizId, user.getName());
				Slack.instance().sendMessage("Quiz Copy for quiz ID " + quizId + " retrieved by " + user.getName()
						+ " at " + dateFormatGmt.format(new Date()));
				
				// Render the output for the club member
				QuizContext context = new QuizContext(quiz, config);
				PDFGenerator generator = new PDFGenerator(context);
				ByteArrayInputStream inputStream = generator.generate(quiz);
				
				return Response.ok().type("application/pdf").entity(inputStream).build();
			}
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
			name = questionDir + "/r-" + recipe.getName() + ".json";
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
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

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
	@Path("uploadImage")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadImage(@CookieParam("quiz.token") Cookie cookie, 
			@FormDataParam("filename") String filename,
			@FormDataParam("file") final InputStream fileInputStream,
			@Context HttpHeaders headers) throws AuthenticationException  {

		int status = 200;
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Make the directory if it doesn't exist
		new File(dataDir).mkdir();
		new File(imageDir).mkdir();

		// Fix the file name so that it is Linux-friendly and does not
		// exhibit the perversion of having spaces in the name.
		filename = filename.replace(" ", "_");
		
		// Create object for the return values
		Map<String, Object> result = new HashMap<String,Object>();
		LOG.info("Uploading image file : {}", filename);
		
		File file = new File(imageDir + "/" + filename);
		LOG.info("Checking to see if '{}' exists", imageDir + "/" + filename);
		
		if ( ! file.exists()) {
			try {
				java.nio.file.Path outputPath = FileSystems.getDefault().getPath(imageDir, filename);
				Files.copy(fileInputStream, outputPath);
				result.put("success", true);
				status = 200;
				LOG.info("Image file upload succeeded.");
				Flash.add(Flash.Code.SUCCESS, "Image was uploaded successfully.");
			} catch (IOException ex) {
				result.put("success", false);
				result.put("error", ex.getClass().getSimpleName() + " : " + filename);
				result.put("preventRetry", true);
				status = 500;
				LOG.info("Image file upload failed.");
				Flash.add(Flash.Code.ERROR, "Image failed to upload.");
			}
		} else {
			result.put("success", false);
			result.put("error", "Image by that name already exists.");
			result.put("preventRetry", true);
			status = 500;
			LOG.info("Image by that name already exists.");
			Flash.add(Flash.Code.ERROR, "Image by that name already exists.");	
		}
		
		return Response.status(status).entity(result).build();
	}
	
	@POST
	@Path("deleteImage")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteImage(@CookieParam("quiz.token") Cookie cookie, 
			@FormDataParam("filename") String filename) throws AuthenticationException  {

		int status = 200;
		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Create object for the return values
		Map<String, Object> result = new HashMap<String,Object>();
		
		try {
			java.nio.file.Path outputPath = FileSystems.getDefault().getPath(imageDir, filename);
			Files.deleteIfExists(outputPath);
			result.put("success", true);
			status = 200;
			Flash.add(Flash.Code.SUCCESS, "Image was deleted successfully.");
		} catch (IOException ex) {
			result.put("success", false);
			result.put("error", ex.getClass().getSimpleName() + " : " + filename);
			result.put("preventRetry", true);
			status = 500;
			Flash.add(Flash.Code.ERROR, "Image failed to delete.");
		}
		
		return Response.status(status).entity(result).build();
	}
	
	@POST
	@Path("addQuestion")
	@Produces(MediaType.TEXT_HTML)
	public Response addQuestion(@CookieParam("quiz.token") Cookie cookie, @FormParam("type") String typeName,
			@FormParam("exclusion") Long exclusionId, @FormParam("required") Boolean required, 
			@FormParam("category") String categoryName, @FormParam("question") String question,
			@FormParam("discussion") String discussion, @FormParam("references") String references,
			@FormParam("difficulty") String difficulty, @FormParam("attributes") List<String> attributes,
			@FormParam("answer") List<String> answers, @FormParam("correct") List<Integer> correct,
			@FormParam("attachment") String attachment)
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
			newQuestion = createQuestion(cookie, typeName, categoryName, exclusionId, required, question, discussion, references, difficulty,
					attributes, answers, null, attachment);
		} else {
			if (correct == null || correct.size() == 0) {
				Flash.add(Flash.Code.ERROR, "A question may not be saved with no correct answer set.");
				return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
			}
			newQuestion = createQuestion(cookie, typeName, categoryName, exclusionId, required, question, discussion, references, difficulty,
					attributes, answers, correct.get(0), attachment);
		}

		return new ViewQuestionResponse(newQuestion.getQuestionId()).cookie(authUtils.generateCookie(user)).build();
	}

	@POST
	@Path("updateQuestion")
	@Produces("text/html")
	public Response updateQuestion(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("exclusion") Long exclusionId,
			@FormParam("questionId") Long questionId, @FormParam("quarantined") Boolean quarantined, 
			@FormParam("required") Boolean required, 
			@FormParam("type") String typeName, @FormParam("category") String categoryName,
			@FormParam("question") String question, @FormParam("discussion") String discussion,
			@FormParam("references") String references, @FormParam("difficulty") String difficulty,
			@FormParam("attributes") List<String> attributes, @FormParam("answer") List<String> answers,
			@FormParam("correct") List<Integer> correct, @FormParam("overwrite") Boolean overwrite,
			@FormParam("attachment") String attachment)
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
		LOG.debug("Attributes --> {}", attributes);

		// Get the existing question, and see if it has been deployed
		Question original = Question.getByQuestionId(questionId);
		// Pick up the correct answer
		if (!typeName.contentEquals("BLANK")) {
			correctAnswer = correct.get(0);
		}

		if (original != null) {
			if ((!original.isDeployed()) || overwrite == true) {
				// Update required status, detecting change
				if (original.isRequired() != required) {
					original.setRequired(required);
					changed = true;
				}
				
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

				// Only save attachments with real names.
				if (attachment.isEmpty()) {
					attachment = "NONE";
				}

				// Then see if the attachment has changed
				if ( ! attachment.equals(original.getAttachment())) {
					changed = true;
				}
				
				// See if quarantined status has changed
				if ( original.isQuarantined() != quarantined) {
					original.setQuarantined(quarantined);
					changed = true;
				}
				
				if ( ! categoryName.equals(original.getCategory().toString())) {
					original.setCategory(Category.valueOf(categoryName.toUpperCase()));
					changed = true;
				}
				
				// Update user-changeable details, detecting changes
				QuestionDetails details = new QuestionDetails(question, discussion, references, answers, correctAnswer, attachment);
				if (details.compareTo(original.getDetails()) != 0) {
					original.setDetails(details);
					changed = true;
				}

				// See if our exclusion group has changed
				if (original.getExclusionId() != exclusionId) {
					// See if we need to remove the question from the old exclusion group
					if (original.getExclusionId() != 0 && original.getExclusionId() != null) {
						ExclusionGroup oldGroup = ExclusionGroup.getByGroupId(original.getExclusionId());
						oldGroup.removeGroupId(questionId);
						oldGroup.save();
					}
					// Now add the question to the new exclusion group, if one is selected
					if (exclusionId != 0) {
						ExclusionGroup exclusionGroup = ExclusionGroup.getByGroupId(exclusionId);
						exclusionGroup.addQuestionId(questionId);
						exclusionGroup.save();
					}
					
					// Now set the new Exclusion ID
					original.setExclusionId(exclusionId);
					changed = true;
				}
				
				if (changed) {
					LOG.info("Updated Question : {}", original.getQuestionId());
					original.save();
					Slack.instance().sendMessage("Updated Question : " + original.getQuestionId());
				} else {
					LOG.info("No changes detected for : {}", original.getQuestionId());
				}

				if (changed) {
					Flash.add(Flash.Code.SUCCESS, "Updated existing question with ID : " + original.getQuestionId());
				} else {
					Flash.add(Flash.Code.SUCCESS, "No changes made to question with ID : " + original.getQuestionId());
				}
			} else {
				Question q = createQuestion(cookie, typeName, categoryName, exclusionId, required, question, discussion, references,
						difficulty, attributes, answers, correctAnswer, attachment);
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
	
	private Question createQuestion(Cookie cookie, String typeName, String categoryName, Long exclusionId, Boolean required, String question,
			String discussion, String references, String difficulty, List<String> attributes, List<String> answers,
			Integer correct, String attachment) throws Exception {

		Question newQuestion;

		LOG.debug("Type       --> {}", typeName);
		LOG.debug("Category   --> {}", categoryName);
		LOG.debug("Exclusion  --> {}", exclusionId);
		LOG.debug("Required   --> {}", required);
		LOG.debug("Question   --> {}", question);
		LOG.debug("Discussion --> {}", discussion);
		LOG.debug("References --> {}", references);
		LOG.debug("Attributes --> {}", attributes);
		LOG.debug("Attachment --> {}", attachment);

		// Add the difficulty to the attributes
		attributes.add(difficulty);
		LOG.debug("Attributes --> {}", attributes);

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
			newQuestion = new Question(type, category, attributes, required,
					new QuestionDetails(question, discussion, references, answers, attachment));
		} else {
			newQuestion = new Question(type, category, attributes, required,
					new QuestionDetails(question, discussion, references, answers, correct, attachment));
		}
		
		// Handle adding to an exclusion group
		if (exclusionId != 0) {
			newQuestion.setExclusionId(exclusionId);
			ExclusionGroup exclusionGroup = ExclusionGroup.getByGroupId(exclusionId);
			exclusionGroup.addQuestionId(newQuestion.getQuestionId());
			exclusionGroup.save();
		}
		
		Slack.instance().sendMessage("Created Question : " + newQuestion.getQuestionId());
		LOG.info("Created Question : {}", newQuestion.getQuestionId());
		newQuestion.save();

		Flash.add(Flash.Code.SUCCESS, "Created new question with ID : " + newQuestion.getQuestionId());
		return newQuestion;
	}

	@POST
	@Path("exclusion")
	@Produces("text/html")
	public Response createExclusion(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("name") String name,
			@FormParam("description") String description)
			throws Exception, AuthenticationException {

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User.getWithClaims(claims);

		if (ExclusionGroup.getByName(name) == null) {
			ExclusionGroup group = new ExclusionGroup(name, description);
			group.save();
		} else {
			return Response.status(401).entity("Group already exists").build();
		}

		return new RedirectResponse(Pages.HOME_PAGE).build();
	}

	@GET
	@Path("exclusion")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getExclusion() {
		List<ExclusionGroup> groups = ExclusionGroup.getAllGroups();
		return Response.ok().entity(groups).build();
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
		if (question.isSuperseded() || question.isDeployed() == true) {
			Flash.add(Flash.Code.ERROR,
					"Question " + question.getQuestionId() + " is either superseded or deployed so is marked for future deletion.");
			question.setDeleted(true);
			question.save();
			return new RedirectResponse(referer).cookie(authUtils.generateCookie(user)).build();
		} else {
			// Remove it from any exclusion groups
			if (question.getExclusionId() != null && question.getExclusionId() != 0) {
				ExclusionGroup exclusionGroup = ExclusionGroup.getByGroupId(question.getExclusionId());
				exclusionGroup.removeGroupId(questionId);
				exclusionGroup.save();
			}
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
	@Path("recipe")
	@Produces(MediaType.TEXT_HTML)
	public Response updateRecipe(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("name") String name,
			@FormParam("recipeId") Long recipeId,
			@FormParam("script") String script
			) throws Exception, AuthenticationException {
		
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
			User user = User.getWithClaims(claims);
	
			// Retrieve the recipe
			Recipe recipe = Recipe.getRecipeById(recipeId);
	
			// Create a new recipe, if needed
			if (recipe == null) {
				recipe = new Recipe();
				recipe.setName(name);
			}
			recipe.setScript(script);
			
			// Save the recipe
			recipe.save();
	
			Flash.add(Flash.Code.SUCCESS, "Recipe type " + recipe.getName() + " updated.");
	
			return new RedirectResponse(Pages.RECIPE_PAGE, recipeId).cookie(authUtils.generateCookie(user)).build();
		} else {
			return Response.status(401).entity("Not authorized.").build();
		}
	}

	@GET
	@Path("recipe/{recipeId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRecipe(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("recipeId") Long recipeId)
			throws Exception, AuthenticationException {

		Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
		User user = User.getWithClaims(claims);

		// Retrieve the recipe
		Recipe recipe  = Recipe.getRecipeById(recipeId);
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
	public Response attributes(@PathParam("category") String category) {
		List<String> attributes = Attribute.attributes(category.toUpperCase());
		return Response.ok().entity(attributes).build();
	}

	@POST
	@Path("script")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response script(Map<String, String> request) {
		String result;
		String status = "success";
		String script = request.get("script");
		Map<String, String> response = new HashMap<String, String>();
		
		Quiz quiz = new Quiz();
		quiz.setQuizName("TEST");
		QuizContext context= new QuizContext(quiz, config);
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
        	ps.println("<li><a href=\"/question/" + question.getQuestionId() + "\" target=\"_blank\">" + question.getQuestionId() + "</a> : " + trim(question.getQuestion()));
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
					name = "quizdata/r-" + recipe.getName() + ".json";
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

	@POST
	@Path("submitFeedback")
	@Produces(MediaType.TEXT_HTML)
	public Response submitFeedback(@CookieParam("quiz.token") Cookie cookie, @FormParam("userId") String userId,
			@FormParam("questionId") Long questionId, @FormParam("feedback") String feedback)
			throws Exception, AuthenticationException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
	
			LOG.debug("User       : {}", user.getName());
			LOG.debug("UserId     : {}", userId);
			LOG.debug("QuestionId : {}", questionId);
			LOG.debug("Feedback   : {}", feedback);
			Slack.instance().sendFeedback(user, questionId, feedback);
			
			QuestionWrapper wrapper = new QuestionWrapper(user);
			String output = renderer.render("thanks.ad", wrapper).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
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
