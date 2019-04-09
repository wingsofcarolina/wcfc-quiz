package org.wingsofcarolina.quiz.resources;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import org.wingsofcarolina.quiz.common.FlashMessage;
import org.wingsofcarolina.quiz.common.Pages;
import org.wingsofcarolina.quiz.common.Templates;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.quiz.Quiz;
import org.wingsofcarolina.quiz.responses.LoginResponse;
import org.wingsofcarolina.quiz.responses.RedirectResponse;

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

	public QuizAPI(QuizConfiguration config) throws IOException {
		this.config = config;
		authUtils = new AuthUtils();
		
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
			FlashMessage.set("Either email or password missing, try again");
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
		}

		String storedPassword = getUserCredentials(email); // users.get(user);
		LOG.debug("Stored Password : {}", storedPassword);
		try {
			if (storedPassword == null || !HashUtils.validatePassword(password, storedPassword)) {
				LOG.info("User not found : {}", email);
				FlashMessage.set("User not found, try again");
				return new RedirectResponse(Pages.LOGIN_PAGE).build();
			}
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
			FlashMessage.set("User not found, try again");
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
				FlashMessage.set("No email or password provided, try again");
				return new RedirectResponse(Pages.REGISTER_PAGE).build();
			} else if (! password.equals(passwordVerify)) {
				FlashMessage.set("Passwords do not match");
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
		if (type.equals("Key")) {
			Quiz quiz = Quiz.quizFromRecord(record);
			output = renderFreemarker(Templates.KEY, quiz).toString();
		} else {
			
		}
		return Response.ok().entity(output).build();
	}

	@POST
	@Path("editQuestion")
	@Produces("text/html")
	public Response editQuestion(@FormParam("question") String question,
			@FormParam("discussion") String discussion,
			@FormParam("attributes") List<String> attributes) throws Exception {
		LOG.info("Question   --> {}", question);
		LOG.info("Discussion --> {}", discussion);
		LOG.info("Attributes --> {}", attributes);
		return Response.ok().build();
	}
	
	private String getUserCredentials(String email) {
		User user = User.getByEmail(email);
		if (user != null) {
			return user.getPassword();
		} else {
			return null;
		}
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
