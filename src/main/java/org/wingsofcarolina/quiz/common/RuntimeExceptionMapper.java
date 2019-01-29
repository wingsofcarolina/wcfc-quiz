package org.wingsofcarolina.quiz.common;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {
	private static final Logger LOG = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

    @Override
    public Response toResponse(RuntimeException exception) {
    	Integer code = 500;
    	if (exception instanceof NotFoundException) {
    		code = 404;
    	}
    	
    	LOG.info("{} : {} : {}", code, exception.getClass().getSimpleName(), exception.getMessage());
    	
    	exception.printStackTrace();
    	
        return Response
                .serverError()
                .entity(new Error(code, exception.getClass().getSimpleName() + " : " + exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}