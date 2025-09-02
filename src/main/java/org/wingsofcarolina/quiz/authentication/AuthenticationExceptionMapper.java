package org.wingsofcarolina.quiz.authentication;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.common.Pages;
import org.wingsofcarolina.quiz.responses.RedirectResponse;

@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {
	private static final Logger LOG = LoggerFactory.getLogger(AuthenticationExceptionMapper.class);

    public Response toResponse(AuthenticationException exception) {
    	LOG.info("Authentication exception for : {}", exception.getMessage());
		NewCookie newCookie = new NewCookie("quiz.token", "", "/", "", "Quiz Login Token", 0, false);

		return new RedirectResponse(Pages.LOGIN_PAGE).cookie(newCookie).build();

    }
}
