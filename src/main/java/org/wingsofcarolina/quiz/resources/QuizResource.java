package org.wingsofcarolina.quiz.resources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TimeZone;

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
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
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
import org.wingsofcarolina.quiz.domain.*;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.presentation.AttributeReportWrapper;
import org.wingsofcarolina.quiz.domain.presentation.AttributeChartWrapper;
import org.wingsofcarolina.quiz.domain.presentation.CategoryReportWrapper;
import org.wingsofcarolina.quiz.domain.presentation.FileListWrapper;
import org.wingsofcarolina.quiz.domain.presentation.JsonWrapper;
import org.wingsofcarolina.quiz.domain.presentation.PDFGenerator;
import org.wingsofcarolina.quiz.domain.presentation.QuestionListWrapper;
import org.wingsofcarolina.quiz.domain.presentation.QuestionWrapper;
import org.wingsofcarolina.quiz.domain.presentation.QuizBuildErrorWrapper;
import org.wingsofcarolina.quiz.domain.presentation.RecipeWrapper;
import org.wingsofcarolina.quiz.domain.presentation.RecordListWrapper;
import org.wingsofcarolina.quiz.domain.presentation.Renderer;
import org.wingsofcarolina.quiz.domain.presentation.UsersWrapper;
import org.wingsofcarolina.quiz.domain.presentation.Version;
import org.wingsofcarolina.quiz.domain.presentation.Wrapper;
import org.wingsofcarolina.quiz.responses.RedirectResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/**
 * @author dwight
 *
 */
@Path("/")
public class QuizResource {
	private static final Logger LOG = LoggerFactory.getLogger(QuizResource.class);

	private static QuizResource instance;
	
	@Context
	UriInfo uri;
	
	private QuizConfiguration config;	// Dropwizard configuration
	private AuthUtils authUtils;

	private Renderer renderer;
	
	ObjectMapper objectMapper;
	Map<String, Object> buildProperties = null;

	private SimpleDateFormat dateFormatGmt;
	private static final Integer pageCount = 10;

	@SuppressWarnings("unchecked")
	public QuizResource(QuizConfiguration config) throws IOException {
		QuizResource.instance = this;

		this.config = config;
		authUtils = new AuthUtils();

		renderer = new Renderer(config);
		
		// Load up all the system git/build properties
		objectMapper = new ObjectMapper();
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
	    ClassLoader classLoader = getClass().getClassLoader();
	    InputStream inputStream = classLoader.getResourceAsStream("git.properties");
	    try {
			buildProperties = objectMapper.readValue(inputStream, Map.class);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	    
		// Get the startup date/time format in GMT
	    dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
		dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public static QuizResource instance() {
		return instance;
	}
	
	@GET
	@Path("favicon.ico")
	public Response favIcon() {
		return new RedirectResponse("/static/favicon.ico").build();
	}
	
	@GET
	@Path("version")
	public Response versionInformation(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException, IOException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			Slack.instance().sendMessage("Version information requested by : " + user.getName() + "(" + user.getEmail() + ")");
			String output = objectMapper.writeValueAsString(buildProperties);
			output = output.replaceAll("(\r\n|\n)", "<br/>");
			output = output.replaceAll("\\s", "&nbsp;&nbsp;");
					
			String rendered = renderer.render("version.ad", new JsonWrapper(user, output)).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(rendered).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
		} else {
			Flash.add(Flash.Code.ERROR, "Something went wrong generating version data.");
			return new RedirectResponse(Pages.HOME_PAGE).build();
		}
	}
	
	@GET
	@Path("gallery")
	public Response gallery(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException, IOException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			
			String imageDir = config.getImageDirectory();
			
			File folder = new File(imageDir);
			File[] listOfFiles = folder.listFiles();
			if (listOfFiles != null && listOfFiles.length > 0) {
				Arrays.sort(listOfFiles, Comparator.comparing(File::getName));
			}

			String rendered = renderer.render("gallery.ad",
					new FileListWrapper(user, config.getImageRoot(), listOfFiles)).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(rendered).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
		} else {
			Flash.add(Flash.Code.ERROR, "Something went wrong generating version data.");
			return new RedirectResponse(Pages.HOME_PAGE).build();
		}
	}
	
	@GET
	@Produces("text/html")
	public Response home(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			if (user != null) {
				String output = renderer.render("home.ad", new Wrapper(user));
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("createExclusion")
	@Produces("text/html")
	public Response createExclusion(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			if (user != null) {
				String output = renderer.render("createExclusion.ad", new Wrapper(user));
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.HOME_PAGE).build();
		}
	}

	@GET
	@Path("login")
	@Produces("text/html")
	public Response login() throws Exception {
		String output = "";
		try {
			output = renderer.render(Templates.LOGIN, new Version(buildProperties)).toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Response.ok().entity(output).build();
	}

	@GET
	@Path("register")
	@Produces("text/html")
	public Response register() throws Exception {
		String output = "";
		try {
			output = renderer.render(Templates.REGISTER, new Object()).toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Response.ok().entity(output).build();
	}

	@GET
	@Path("showUsers")
	@Produces("text/html")
	public Response showUsers(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		String output = "";
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			try {
				UsersWrapper wrapper = new UsersWrapper(user, User.getAllUsers());
				output = renderer.render(Templates.SHOW_USERS, wrapper).toString();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return Response.ok().entity(output).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("profile")
	@Produces("text/html")
	public Response profile(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			if (user != null) {
				String output = renderer.render("profile.ad", new Wrapper(user)).toString();
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("generate/{alias}")
	@Produces("text/html")
	public Response generate(
			@PathParam("alias") String alias) throws Exception {
		Recipe recipe = null;
		Quiz quiz = null;
		
		if (StringUtils.isNumeric(alias)) {
			// Assumme we are asking for the Recipe by ID
			Long recipeId = Long.parseLong(alias);
			recipe = Recipe.getRecipeById(recipeId);
		} else {
			recipe = Recipe.getRecipeByAlias(alias.toUpperCase());
		}
		
		if (recipe != null) {
			String output = "";
			try {
				// Build the quiz itself
				quiz = new Quiz(recipe);
				QuizContext context = new QuizContext(quiz, config);
				quiz.build(context);
	
				// Render the output for the club member
				PDFGenerator generator = new PDFGenerator(context);
	
				// Store the quiz question set for later retrieval
				Record record = quiz.getRecord();
				record.save();
				LOG.info(record.toString());
	
				// Actually perform the PDF quiz generation
				// Slack.instance().sendMessage("Quiz '" + quiz.getQuizName() + "', ID " +  quiz.getQuizId() + ", requested at " + dateFormatGmt.format(new Date()));
				ByteArrayInputStream inputStream = generator.generate(quiz);
				
				return Response.ok().type("application/pdf").entity(inputStream).build();
			} catch (QuizBuildException e) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				output = mapper.writeValueAsString(recipe);
				output = output.replaceAll("(\r\n|\n)", "<br/>");
				output = output.replaceAll("\\s", "&nbsp;&nbsp;");
	
				// Notify someone of the failure to generate a quiz. This is critical!
				Slack.instance().sendMessage("ERROR generating quiz for " + quiz.getQuizName() + " : " + e.getMessage());
				
				QuizBuildErrorWrapper wrapper = new QuizBuildErrorWrapper(e.getMessage(), output);
				output = renderer.render("quizBuildError.ad", wrapper);
				return Response.ok().entity(output).build();
			}
		} else {
			return Response.status(404).entity("Recipe not found").build();
		}
	}
	
	@GET
	@Path("previewQuestion/{id}")
	@Produces("application/pdf")
	public Response previewQuestion(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("id") Long questionId) throws Exception, AuthenticationException {
		// Render the preview of the question
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			Question question = Question.getByQuestionId(questionId);
			if (question != null) {
				PDFGenerator generator = new PDFGenerator(new QuizContext(new Quiz(), config));
		
				ByteArrayInputStream inputStream = generator.generate(question);
				return Response.ok().type("application/pdf").entity(inputStream).build();
			} else {
				Flash.add(Flash.Code.ERROR, "Requested question \"" + questionId + "\" not found.");
				NewCookie newCookie = authUtils.generateCookie(user);
				return new RedirectResponse(Pages.HOME_PAGE).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("previewKey/{id}")
	@Produces("text/html")
	public Response previewKey(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("id") Long questionId) throws Exception, AuthenticationException {
		// Render the preview of the question key
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			try
			{
				Question question = Question.getByQuestionId(questionId);
				if (question != null ) {			
					// We have found nulls in the attributes, so lets sanitize the list
					// until we find the root cause.
					List<String> attributes = question.getAttributes();
				    for (ListIterator<String> iterator = attributes.listIterator(); iterator.hasNext();) {
				    	if (iterator.next() == null) {
				    		iterator.remove();
				    	}
				    }

					QuestionWrapper wrapper = new QuestionWrapper(user, question);
					String output = renderer.render(Templates.PREVIEW_KEY, wrapper).toString();
					NewCookie newCookie = authUtils.generateCookie(user);
					return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
				} else {
					Flash.add(Flash.Code.ERROR, "Requested question \"" + questionId + "\" not found.");
					NewCookie newCookie = authUtils.generateCookie(user);
					return new RedirectResponse(Pages.HOME_PAGE).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
				}
			} catch (NumberFormatException ex) {
				Flash.add(Flash.Code.ERROR, "Invalid question ID \"" + questionId + "\" entered.");
				NewCookie newCookie = authUtils.generateCookie(user);
				return new RedirectResponse(Pages.HOME_PAGE).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}

	}
	
	@GET
	@Path("question/{id}")
	@Produces("text/html")
	public Response showQuestion(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("id") String id) throws AuthenticationException, IOException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			try
			{
				Long questionId = Long.valueOf(id);
				Question question = Question.getByQuestionId(questionId);
				if (question != null ) {			
					// We have found nulls in the attributes, so lets sanitize the list
					// until we find the root cause.
					List<String> attributes = question.getAttributes();
				    for (ListIterator<String> iterator = attributes.listIterator(); iterator.hasNext();) {
				    	if (iterator.next() == null) {
				    		iterator.remove();
				    	}
				    }

					QuestionWrapper wrapper = new QuestionWrapper(user, question);
					String output = renderer.render("showQuestion.ad", wrapper).toString();
					NewCookie newCookie = authUtils.generateCookie(user);
					return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
				} else {
					Flash.add(Flash.Code.ERROR, "Requested question \"" + id + "\" not found.");
					NewCookie newCookie = authUtils.generateCookie(user);
					return new RedirectResponse(Pages.HOME_PAGE).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
				}
			} catch (NumberFormatException ex) {
				Flash.add(Flash.Code.ERROR, "Invalid question ID \"" + id + "\" entered.");
				NewCookie newCookie = authUtils.generateCookie(user);
				return new RedirectResponse(Pages.HOME_PAGE).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("report/attribute/{attribute}")
	@Produces("text/html")
	public Response reportAttribute(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("attribute") String attribute,
			@QueryParam("alphabetical") Boolean alphabetical,
			@QueryParam("superseded") Boolean superseded) throws AuthenticationException, IOException {

		// We only check to see if ?alphabetical is in the URL, not the value
		// of the parameter. IF it exists at all, we sort, otherwise we don't.
		alphabetical = alphabetical == null ? false : true;
		superseded = superseded == null ? false : true;
		
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			List<Question> questions = Question.getByAttribute(attribute);
			
			// Weed out any superseded questions.
			if ( ! superseded) {
				Iterator<Question> it = questions.iterator();
				while (it.hasNext()) {
					Question question = it.next();
					if (question.isSuperseded()) {
						it.remove();
					}
				}
			}
			
			// Optionally, sort alphabetically by question
			if (alphabetical) {
				Question[] questionArray = (Question[]) questions.toArray(new Question[0]);
				Arrays.sort(questionArray, Comparator.comparing(Question::getQuestion));
				questions = Arrays.asList(questionArray);
			}

			AttributeReportWrapper wrapper = new AttributeReportWrapper(user, questions, attribute);
			String output = renderer.render("reportAttribute.ad", wrapper).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("report/category/{category}")
	@Produces("text/html")
	public Response reportCategory(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("category") String category,
			@QueryParam("alphabetical") Boolean alphabetical,
			@QueryParam("superseded") Boolean superseded) throws AuthenticationException, IOException {

		// We only check to see if ?alphabetical is in the URL, not the value
		// of the parameter. IF it exists at all, we sort, otherwise we don't.
		alphabetical = alphabetical == null ? false : true;
		superseded = superseded == null ? false : true;
		
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			List<Question> questions = Question.getByCategory(Category.valueOf(category));
			
			// Weed out any superseded questions.
			if ( ! superseded) {
				Iterator<Question> it = questions.iterator();
				while (it.hasNext()) {
					Question question = it.next();
					if (question.isSuperseded()) {
						it.remove();
					}
				}
			}
			
			// Optionally, sort alphabetically by question
			if (alphabetical) {
				Question[] questionArray = (Question[]) questions.toArray(new Question[0]);
				Arrays.sort(questionArray, Comparator.comparing(Question::getQuestion));
				questions = Arrays.asList(questionArray);
			}

			CategoryReportWrapper wrapper = new CategoryReportWrapper(user, questions, category);
			String output = renderer.render("reportCategory.ad", wrapper).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("quarantinedReport")
	@Produces("text/html")
	public Response quarantinedReport(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException, IOException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			List<Question> questions = Question.getAllQuarantined();
			QuestionListWrapper wrapper = new QuestionListWrapper(user, questions);
			String output = renderer.render("quarantinedReport.ad", wrapper).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("recordReport")
	@Produces("text/html")
	public Response recordReport(@CookieParam("quiz.token") Cookie cookie) throws AuthenticationException, IOException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			List<Record> questions = Record.getAllRecords();
			RecordListWrapper wrapper = new RecordListWrapper(user, questions);
			String output = renderer.render("recordReport.ad", wrapper).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("chart/{attribute}")
	@Produces("text/html")
	public Response chartAttribute(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("attribute") String attribute) throws AuthenticationException, IOException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			List<Question> questions = Question.getByAttribute(attribute);
			
			AttributeChartWrapper wrapper = new AttributeChartWrapper(user, questions, attribute);
			
			String output = renderer.render("chartAttribute.ad", wrapper).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	

	@GET
	@Path("chart/category/{category}")
	@Produces("text/html")
	public Response chartCategory(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("category") String category) throws AuthenticationException, IOException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			List<Question> questions = Question.getByCategory(Category.valueOf(category));
			
			AttributeChartWrapper wrapper = new AttributeChartWrapper(user, questions, category);
			
			String output = renderer.render("chartAttribute.ad", wrapper).toString();
			NewCookie newCookie = authUtils.generateCookie(user);
			return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	@GET
	@Path("allQuestions/{attribute}")
	@Produces("application/pdf")
	public Response allQuestionsWithAttribute(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("attribute") String attribute) throws AuthenticationException, IOException, QuizBuildException, URISyntaxException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			List<Question> questions = Question.getByAttribute(attribute);
			int index = 1;
			for (Question question : questions) {
				question.setIndex(index++);
			}
			
			if (questions != null) {
//				PDFGenerator generator = new PDFGenerator(new QuizContext(new Quiz(), config));
//		
//				ByteArrayInputStream inputStream = generator.generate(questions);
//				return Response.ok().type("application/pdf").entity(inputStream).build();
				Quiz quiz = new Quiz();
				quiz.addAll(questions);
				quiz.setQuizName("All " + attribute + " questions");
				quiz.setQuizId(0l);
				String output = renderer.render(Templates.KEY, quiz).toString();
				return Response.ok().entity(output).type("text/html").build();
			} else {
				Flash.add(Flash.Code.ERROR, "Questions with requested attribute \"" + attribute + "\" not found.");
				NewCookie newCookie = authUtils.generateCookie(user);
				return new RedirectResponse(Pages.HOME_PAGE).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("allQuestions/category/{category}")
	@Produces("application/pdf")
	public Response allQuestionsInCategory(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("category") String category) throws AuthenticationException, IOException, QuizBuildException, URISyntaxException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			List<Question> questions = Question.getByCategory(Category.valueOf(category));
			int index = 1;
			for (Question question : questions) {
				question.setIndex(index++);
			}
			
			if (questions != null) {
//				PDFGenerator generator = new PDFGenerator(new QuizContext(new Quiz(), config));
//		
//				ByteArrayInputStream inputStream = generator.generate(questions);
//				return Response.ok().type("application/pdf").entity(inputStream).build();
				Quiz quiz = new Quiz();
				quiz.addAll(questions);
				quiz.setQuizName("All " + category + " questions");
				quiz.setQuizId(0l);
				String output = renderer.render(Templates.KEY, quiz).toString();
				return Response.ok().entity(output).type("text/html").build();
			} else {
				Flash.add(Flash.Code.ERROR, "Questions in requested category \"" + category + "\" not found.");
				NewCookie newCookie = authUtils.generateCookie(user);
				return new RedirectResponse(Pages.HOME_PAGE).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("recipe/{name}")
	@Produces("text/html")
	public Response showRecipe(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("name") String name,
			@QueryParam("attribute") String attribute) throws Exception, AuthenticationException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			
			String output = "";
			Recipe recipe = Recipe.getRecipe(name);
			
			if (recipe != null) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				output = mapper.writeValueAsString(recipe);
				output = output.replaceAll("(\r\n|\n)", "<br/>");
				output = output.replaceAll("\\s", "&nbsp;&nbsp;");
						
				String rendered = renderer.render("recipe.ad", new JsonWrapper(user, output)).toString();
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(rendered).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				Flash.add(Flash.Code.ERROR, "Requested recipe for \"" + name + "\" not found.");
				NewCookie newCookie = authUtils.generateCookie(user);
				return new RedirectResponse(Pages.HOME_PAGE).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("addQuestion")
	@Produces("text/html")
	public Response addQuestion(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
			User user = User.getWithClaims(claims);
			if (user != null) {
				QuestionWrapper wrapper = new QuestionWrapper(user);
				String output = renderer.render("addQuestion.ad", wrapper).toString();
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("feedback/{questionId}")
	@Produces("text/html")
	public Response feedback(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("questionId") Long questionId) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			if (user != null) {
				Question question = Question.getByQuestionId(questionId);
				QuestionWrapper wrapper = new QuestionWrapper(user, question);
				String output = renderer.render("feedback.ad", wrapper).toString();
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("browseQuestions")
	@Produces("text/html")
	public Response browseFirstQuestions(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		return browseQuestions(cookie, 0);
	}
	
	@GET
	@Path("browseQuestions/{skip}")
	@Produces("text/html")
	public Response browseNextQuestions(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("skip") Integer skip) throws Exception, AuthenticationException {
		return browseQuestions(cookie, skip);
	}
	
	@POST
	@Path("browseQuestions")
	@Produces("text/html")
	public Response browseQuestionsSkip(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("questionId") Integer questionId) throws Exception, AuthenticationException {
		int skip = questionId - 1000;
		skip = skip < 0 ? 0 : skip;
		return browseQuestions(cookie, skip);
	}
	
	public Response browseQuestions(Cookie cookie, Integer skip) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
			User user = User.getWithClaims(claims);
			if (user != null) {
				List<Question> questions = Question.getQuestionsLimited(skip, pageCount);
				QuestionListWrapper wrapper = new QuestionListWrapper(user, questions, skip, pageCount);
				String output = renderer.render("browseQuestions.ad", wrapper).toString();
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("searchQuestions")
	@Produces("text/html")
	public Response searchFirstQuestions(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		return searchQuestions(cookie, null, false);
	}

	@POST
	@Path("searchQuestions")
	@Produces("text/html")
	public Response searchQuestions(@CookieParam("quiz.token") Cookie cookie,
			@FormParam("search") String search,
			@FormParam("case") Boolean caseSensitive) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.ADMIN);
			User user = User.getWithClaims(claims);
			if (user != null) {
				List<Question> questions = new ArrayList<Question>();
				if (search != null && !search.isEmpty()) {
					List<Question> candidates = Question.getAllQuestions();
					LOG.info("Selected {} candidates", candidates.size());
					for (Question question : candidates) {
						if (compareCase(question.getQuestion(), search, caseSensitive)) {
							questions.add(question);
						} else if (compareCase(question.getDiscussion(), search, caseSensitive)) {
							questions.add(question);
						} else if (compareCase(question.getCategory().name(), search, caseSensitive)) {
							questions.add(question);
						}
					}
				}
				QuestionListWrapper wrapper = new QuestionListWrapper(user, questions);
				String output = renderer.render("searchQuestions.ad", wrapper).toString();
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	private boolean compareCase(String target, String search, boolean caseSensitive) {
		if (!caseSensitive) {
			if (StringUtils.containsIgnoreCase(target, search)) {
				return true;
			}
		} else if (target.contains(search)) {
			return true;
		}
		return false;
	}
	
	@GET
	@Path("updateQuestion/{questionId}")
	@Produces("text/html")
	public Response updateQuestion(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("questionId") Long questionId) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			Question question = Question.getByQuestionId(questionId);
			if (user != null && question != null) {
				QuestionWrapper wrapper = new QuestionWrapper(user, question);
				String output = renderer.render("updateQuestion.ad", wrapper).toString();
				return Response.ok().entity(output).cookie(authUtils.generateCookie(user, question.getQuestionId())).build();
			} else {
				Flash.add(Flash.Code.WARN, "Question with ID " + questionId + " not found.");			
				return new RedirectResponse(Pages.HOME_PAGE).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("updateRecipe")
	@Produces("text/html")
	public Response updateRecipe(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			if (user != null) {
				URI myUri = new URI("https://quiz.wingsofcarolina.org/");
				if (config.getMode().equals("DEV")) {
					myUri = new URI("http://localhost:9314/");
				}
				RecipeWrapper wrapper = new RecipeWrapper(user, myUri);
				String output = renderer.render("updateRecipe.ad", wrapper).toString();
				NewCookie newCookie = authUtils.generateCookie(user);
				return Response.ok().entity(output).header("Set-Cookie", AuthUtils.sameSite(newCookie)).build();
			} else {
				return new RedirectResponse(Pages.LOGIN_PAGE).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	/////////////////////////////
	// Supporting methods 
	/////////////////////////////
	public User addUser(String email) {
		return this.addUser(null, email, null, Privilege.USER);
	}
	
	public User addUser(String name, String email) {
		return this.addUser(name, email, null, Privilege.USER);
	}
	
	public User addUser(String email, String password, Privilege priv) {
		return addUser(null, email, password, priv);
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
				LOG.info("New user  : {}", user);
			} else {
				LOG.info("User '{}' already exists.", user.getEmail());
			}

			return user;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}
	}
}
