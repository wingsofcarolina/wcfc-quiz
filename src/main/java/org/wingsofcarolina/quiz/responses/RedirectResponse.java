package org.wingsofcarolina.quiz.responses;

import java.net.URI;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;

public class RedirectResponse extends AbstractResponse {
	private String uriString;
	private NewCookie cookies = null;

	public RedirectResponse() {
		super();
	}

	public RedirectResponse(String uriString) {
		super("ok");
		this.uriString = uriString;
	}
	
	public RedirectResponse cookie(NewCookie cookie) {
		this.cookies = cookie;
		return this;
	}
	
	public Response build() {
		URI uri = UriBuilder.fromUri(uriString).build();
		ResponseBuilder resp = Response.seeOther(uri);
		if (cookies != null) {
			resp.cookie(cookies);
		}
		return resp.build();
	}
}
