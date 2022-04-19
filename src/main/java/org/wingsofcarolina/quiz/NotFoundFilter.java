package org.wingsofcarolina.quiz;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class NotFoundFilter implements ContainerResponseFilter {
	private static final Logger LOG = LoggerFactory.getLogger(NotFoundFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
        throws IOException {
 
    	if (responseContext.getStatus() == 404) {
    		LOG.info("Failed 404 request provoked by : {}", requestContext.getUriInfo().getAbsolutePath());
    	}
    }
}