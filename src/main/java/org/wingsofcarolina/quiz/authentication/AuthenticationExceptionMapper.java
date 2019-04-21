package org.wingsofcarolina.quiz.authentication;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.common.Pages;
import org.wingsofcarolina.quiz.responses.RedirectResponse;

@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {
	private static final Logger LOG = LoggerFactory.getLogger(AuthenticationExceptionMapper.class);

    public Response toResponse(AuthenticationException exception) {
    	LOG.info("Authentication exception for : {}", exception.getMessage());
		return new RedirectResponse(Pages.HOME_PAGE).build();

    }
}
