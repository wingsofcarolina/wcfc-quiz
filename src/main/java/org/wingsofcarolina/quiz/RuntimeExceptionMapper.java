package org.wingsofcarolina.quiz;

import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.common.Error;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Provider
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {
	private static final Logger LOG = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

	// magically inject a thing
    // remember that magic is for evil wizards
    @Context
    private HttpServletRequest request;
    
    @Override
    public Response toResponse(RuntimeException exception) {
    	Integer code = 500;
    	
    	// Log the request that triggered the error, if it was from an external
    	// trigger and not from some internal boo boo.
		if (request != null) {
            final StringBuffer absolutePath = request.getRequestURL();
            LOG.error("HTTP Request: " + absolutePath);
		}
		
		// Make sure we let the client know if it was something that they asked
		// for that doesn't exist, otherwise it is just a generic server error.
		if (exception instanceof NotFoundException) {
    		code = 404;
    	} else {
    		LOG.info("{} : {} : {}", code, exception.getClass().getSimpleName(), exception.getMessage());
    		exception.printStackTrace();
    	}
    	
        return Response
                .status(code)
                .entity(new Error(code, exception.getClass().getSimpleName() + " : " + exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}