package org.wingsofcarolina.quiz.responses;

import java.net.URI;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;

public class RedirectResponse extends AbstractResponse {
	private String uriString;
	private NewCookie cookies = null;
	private String headerName = null;
	private String headerValue = null;
	
	public RedirectResponse() {
		super();
	}

	public RedirectResponse(String uriString) {
		super("ok");
		this.uriString = uriString;
	}
	
	public RedirectResponse(String uriString, Long recipeId) {
		super("ok");
		this.uriString = uriString + "?recipe=" + recipeId.toString();
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
		if (headerName != null && headerValue != null) {
			resp.header(headerName, headerValue);
		}
		return resp.build();
	}

	public RedirectResponse header(String headerName, String headerValue) {
		this.headerName = headerName;
		this.headerValue = headerValue;
		return this;
	}
}
