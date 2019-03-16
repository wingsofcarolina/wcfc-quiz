package org.wingsofcarolina.quiz.resources;

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
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.responses.LoginResponse;
import org.wingsofcarolina.quiz.responses.RedirectResponse;
import org.wingsofcarolina.quiz.responses.Response404;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

/**
 * @author dwight
 *
 */
@Path("/quiz/api")
public class QuizAPI {
	private static final Logger LOG = LoggerFactory.getLogger(QuizAPI.class);
	@SuppressWarnings("unused")
	private QuizConfiguration config;
	private AuthUtils authUtils;

	public QuizAPI(QuizConfiguration config) {
		this.config = config;
		authUtils = new AuthUtils();
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
	
	private String getUserCredentials(String name) {
		User user = User.getByEmail(name);
		if (user != null) {
			return user.getPassword();
		} else {
			return null;
		}
	}
}
