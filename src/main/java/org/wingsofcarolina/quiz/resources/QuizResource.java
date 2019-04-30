package org.wingsofcarolina.quiz.resources;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.QuizConfiguration;
import org.wingsofcarolina.quiz.authentication.AuthUtils;
import org.wingsofcarolina.quiz.authentication.AuthenticationException;
import org.wingsofcarolina.quiz.authentication.HashUtils;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.common.Pages;
import org.wingsofcarolina.quiz.common.Templates;
import org.wingsofcarolina.quiz.domain.*;
import org.wingsofcarolina.quiz.domain.presentation.QuestionListWrapper;
import org.wingsofcarolina.quiz.domain.presentation.QuestionWrapper;
import org.wingsofcarolina.quiz.domain.presentation.Wrapper;
import org.wingsofcarolina.quiz.resources.Quiz.QuizType;
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

	private static final Integer pageCount = 10;

	public QuizResource(QuizConfiguration config) throws IOException {
		QuizResource.instance = this;

		this.config = config;
		authUtils = new AuthUtils();

		renderer = new Renderer(config);
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
	@Produces("text/html")
	public Response home(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			if (user != null) {
				String output = renderer.render("home.ad", new Wrapper(user));
				return Response.ok().entity(output).build();
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
			output = renderer.render(Templates.LOGIN, new Object()).toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Response.ok().entity(output).build();
	}

	@POST
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
				return Response.ok().entity(output).build();
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
	public Response generate(@QueryParam("quiz") String quizType) throws Exception {
		String output = "";
		try {
			Quiz quiz = new Quiz(quizType).build();
			
			// Store the quiz question set for later retrieval
			Record record = quiz.getRecord();
			record.save();
			
			// Render the output for the club member
			output = renderer.render(Templates.QUIZ, quiz).toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Response.ok().entity(output).build();
	}
	
	@GET
	@Path("recipe")
	@Produces("text/html")
	public Response recipe(@QueryParam("quiz") String quiz) throws Exception {
		Quiz.QuizType quizType = null;

		String output = "";
		switch (quiz) {
		case "far":
			quizType = QuizType.FAR;
			break;
		case "sop-student":
			quizType = QuizType.SOP_STUDENT;
			break;
		case "sop-pilot":
			quizType = QuizType.SOP_PILOT;
			break;
		case "sop-instructor":
			quizType = QuizType.SOP_INSTRUCTOR;
			break;
		case "c152":
			quizType = QuizType.C152;
			break;
		case "c172":
			quizType = QuizType.C172;
			break;
		case "pa28":
			quizType = QuizType.PA28;
			break;
		case "m20j":
			quizType = QuizType.M20J;
			break;
		}
		Recipe recipe = Recipe.getRecipeByType(quizType);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		output = mapper.writeValueAsString(recipe);
		output = output.replaceAll("(\r\n|\n)", "<br/>");
		output = output.replaceAll("\\s", "&nbsp;&nbsp;");
				
		return Response.ok().entity(output).build();
	}
	
	@GET
	@Path("question/{id}")
	@Produces("text/html")
	public Response question(@PathParam("id") String id) throws Exception {

		Question question = Question.getByQuestionId(Long.valueOf(id));
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.enable(SerializationFeature.INDENT_OUTPUT);
		String output = mapper.writeValueAsString(question);
		output = output.replaceAll("(\r\n|\n)", "<br/>");
		output = output.replaceAll("\\s", "&nbsp;&nbsp;");
				
		return Response.ok().entity(output).build();
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
				return Response.ok().entity(output).build();
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
				return Response.ok().entity(output).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

	@GET
	@Path("editQuestion/{questionId}")
	@Produces("text/html")
	public Response editQuestion(@CookieParam("quiz.token") Cookie cookie,
			@PathParam("questionId") Long questionId) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			Question question = Question.getByQuestionId(questionId);
			if (user != null || question == null) {
				QuestionWrapper wrapper = new QuestionWrapper(user, question);
				String output = renderer.render("editQuestion.ad", wrapper).toString();
				return Response.ok().entity(output).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}
	}

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
				user.setFullname(name);
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
