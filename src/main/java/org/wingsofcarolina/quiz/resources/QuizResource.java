package org.wingsofcarolina.quiz.resources;

import static org.asciidoctor.Asciidoctor.Factory.create;
import static org.asciidoctor.AttributesBuilder.attributes;
import static org.asciidoctor.OptionsBuilder.options;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.extension.JavaExtensionRegistry;
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
import org.wingsofcarolina.quiz.domain.quiz.Quiz;
import org.wingsofcarolina.quiz.domain.quiz.Quiz.QuizType;
import org.wingsofcarolina.quiz.extensions.*;
import org.wingsofcarolina.quiz.responses.RedirectResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
@Path("/")
public class QuizResource {
	private static final Logger LOG = LoggerFactory.getLogger(QuizResource.class);

	private static QuizResource instance;
	
	@SuppressWarnings("unused")
	private QuizConfiguration config;	// Dropwizard configuration
	private Configuration freemarker;	// FreeMarker configuration
	
	private Asciidoctor asciidoctor;
	private Map<String, Object> options;
	private AuthUtils authUtils;

	public QuizResource(QuizConfiguration config) throws IOException {
		QuizResource.instance = this;
		
		this.config = config;
		authUtils = new AuthUtils();

		Map<String, Object> userAttributes = new HashMap<String,Object>();
		userAttributes.put("lesson-version","1.2.3");

		asciidoctor = create();
		Map<String, Object> attributes = attributes()
				.linkCss(true)
				.attributes(userAttributes)
				// TODO: Find a better place for the CSS 
				.styleSheetName("http:/static/asciidoctor-default.css")
				.allowUriRead(true).asMap();
		options = options()
				.inPlace(true)
				.backend("html5")
				.headerFooter(true)
				.attributes(attributes).asMap();
		
		// Add custom extensions
		JavaExtensionRegistry extensionRegistry = this.asciidoctor.javaExtensionRegistry(); 
		extensionRegistry.inlineMacro("navbar", NavigationBar.class);
		extensionRegistry.inlineMacro("flash", Flash.class);
		extensionRegistry.inlineMacro("button", Button.class);
		extensionRegistry.inlineMacro("color", Color.class);
		
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
	public Response userHome(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			String output = "";
			if (user != null) {
				output = renderFreemarker("home.ad", user).toString();
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
			String rendered = renderFreemarker(Templates.LOGIN, new Object()).toString();
			output = asciidoctor.convert(rendered, options);
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
			String rendered = renderFreemarker(Templates.REGISTER, new Object()).toString();
			output = asciidoctor.convert(rendered, options);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Response.ok().entity(output).build();
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
			output = renderFreemarker(Templates.QUIZ, quiz).toString();
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
	@Path("editQuestion")
	@Produces("text/html")
	public Response editQuestion(@CookieParam("quiz.token") Cookie cookie) throws Exception, AuthenticationException {
		if (cookie != null) {
			Jws<Claims> claims = authUtils.validateUser(cookie.getValue());
			User user = User.getWithClaims(claims);
			String output = "";
			if (user != null) {
				String rendered = renderFreemarker("editQuestion.ad", user).toString();
				output = asciidoctor.convert(rendered, options);
				return Response.ok().entity(output).build();
			} else {
				return Response.status(404).build();
			}
		} else {
			return new RedirectResponse(Pages.LOGIN_PAGE).build();
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
