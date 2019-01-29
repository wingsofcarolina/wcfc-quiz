package org.wingsofcarolina.quiz.common;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.wingsofcarolina.quiz.authentication.AuthenticationException;

@Provider
public class AuthenticationExceptionMapper implements ExceptionMapper<AuthenticationException> {
    public Response toResponse(AuthenticationException exception) {
        return Response.status(exception.getCode())
                .entity(new Error(exception.getCode(), exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
