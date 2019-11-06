package org.wingsofcarolina.quiz.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;

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
import org.wingsofcarolina.quiz.domain.presentation.CategoryChartWrapper;
import org.wingsofcarolina.quiz.domain.presentation.CategoryReportWrapper;
import org.wingsofcarolina.quiz.domain.presentation.JsonWrapper;
import org.wingsofcarolina.quiz.domain.presentation.PDFGenerator;
import org.wingsofcarolina.quiz.domain.presentation.QuestionListWrapper;
import org.wingsofcarolina.quiz.domain.presentation.QuestionWrapper;
import org.wingsofcarolina.quiz.domain.presentation.QuizBuildErrorWrapper;
import org.wingsofcarolina.quiz.domain.presentation.Renderer;
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
	
	@SuppressWarnings("unused")
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
			return Response.ok().entity(rendered).cookie(authUtils.generateCookie(user)).build();
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
				return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
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
	@Path("profile")
	@Produces("text/html")
	public Response profile(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			if (user != null) {
				String output = renderer.render("profile.ad", new Wrapper(user)).toString();
				return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("generate")
	@Produces("text/html")
	public Response generate(
			@QueryParam("quiz") String category,
			@QueryParam("attribute") String attribute) throws Exception {
		Quiz quiz = null;
		
		String output = "";
		try {
			// Build the quiz itself
			if (attribute == null) {
				quiz = new Quiz(category);
			} else {
				quiz = new Quiz(category, attribute);
			}
			QuizContext context = new QuizContext(quiz, config);
			quiz.build(context);

			// Render the output for the club member
			PDFGenerator generator = new PDFGenerator(context);

			// Store the quiz question set for later retrieval
			Record record = quiz.getRecord();
			record.save();

			Slack.instance().sendMessage("Quiz " + quiz.getQuizName() + " requested at " + dateFormatGmt.format(new Date()));
			ByteArrayInputStream inputStream = generator.generate(quiz);
			return Response.ok().type("application/pdf").entity(inputStream).build();
		} catch (QuizBuildException e) {
			Recipe recipe = Recipe.getRecipeByCategoryAndAttribute(quiz.getCategory(), attribute);
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
	}
	
	@GET
	@Path("previewQuestion/{id}")
	@Produces("text/html")
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
				return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
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
					return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
				} else {
					Flash.add(Flash.Code.ERROR, "Requested question \"" + questionId + "\" not found.");
					return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
				}
			} catch (NumberFormatException ex) {
				Flash.add(Flash.Code.ERROR, "Invalid question ID \"" + questionId + "\" entered.");
				return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
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
					return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
				} else {
					Flash.add(Flash.Code.ERROR, "Requested question \"" + id + "\" not found.");
					return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
				}
			} catch (NumberFormatException ex) {
				Flash.add(Flash.Code.ERROR, "Invalid question ID \"" + id + "\" entered.");
				return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("report/{category}")
	@Produces("text/html")
	public Response reportCategory(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("category") String category) throws AuthenticationException, IOException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			Category cat = Category.valueOf(category);
			List<Question> questions = Question.getSelected(cat);
			CategoryReportWrapper wrapper = new CategoryReportWrapper(user, questions, cat);
			String output = renderer.render("reportCategory.ad", wrapper).toString();
			return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("chart/{category}")
	@Produces("text/html")
	public Response chartCategory(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("category") String category) throws AuthenticationException, IOException {

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			Category cat = Category.valueOf(category);
			List<Question> questions = Question.getSelected(cat);
			
			CategoryChartWrapper wrapper = new CategoryChartWrapper(user, questions, cat);
			
			String output = renderer.render("chartCategory.ad", wrapper).toString();
			return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}
	
	@GET
	@Path("recipe/{quiz}")
	@Produces("text/html")
	public Response showRecipe(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("quiz") String quiz,
			@QueryParam("attribute") String attribute) throws Exception, AuthenticationException {
		Category category = null;

		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue(), Privilege.USER);
			User user = User.getWithClaims(claims);
			
			String output = "";
			switch (quiz.toLowerCase()) {
			case "far":
				category = Category.FAR;
				break;
			case "sop":
				category = Category.SOP;
				break;
			case "c152":
				category = Category.C152;
				break;
			case "c172":
				category = Category.C172;
				break;
			case "pa28":
				category = Category.PA28;
				break;
			case "m20j":
				category = Category.M20J;
				break;
			}
			Recipe recipe = Recipe.getRecipeByCategoryAndAttribute(category, attribute);
			
			if (recipe != null) {
				ObjectMapper mapper = new ObjectMapper();
				mapper.enable(SerializationFeature.INDENT_OUTPUT);
				output = mapper.writeValueAsString(recipe);
				output = output.replaceAll("(\r\n|\n)", "<br/>");
				output = output.replaceAll("\\s", "&nbsp;&nbsp;");
						
				String rendered = renderer.render("recipe.ad", new JsonWrapper(user, output)).toString();
				return Response.ok().entity(rendered).cookie(authUtils.generateCookie(user)).build();
			} else {
				Flash.add(Flash.Code.ERROR, "Requested recipe for \"" + quiz + "\" not found.");
				return new RedirectResponse(Pages.HOME_PAGE).cookie(authUtils.generateCookie(user)).build();
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
				return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
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
				return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
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
				return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
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
				Wrapper wrapper = new Wrapper(user);
				String output = renderer.render("updateRecipe.ad", wrapper).toString();
				return Response.ok().entity(output).cookie(authUtils.generateCookie(user)).build();
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
