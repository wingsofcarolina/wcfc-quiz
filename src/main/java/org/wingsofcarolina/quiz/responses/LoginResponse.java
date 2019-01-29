package org.wingsofcarolina.quiz.responses;

import java.net.URI;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.wingsofcarolina.quiz.common.Pages;

import javax.ws.rs.core.UriBuilder;

public class LoginResponse extends AbstractResponse {

	private NewCookie cookie = null;
	
	public LoginResponse() {
		super();
	}

	public LoginResponse(NewCookie cookie) {
		super("ok");
		this.cookie = cookie;
	}
	
	public Response build() {
		URI uri = UriBuilder.fromUri(Pages.HOME_PAGE).build();
		ResponseBuilder resp = Response.seeOther(uri);
		if (cookie != null) {
			resp.cookie(cookie);
		}
        
		return resp.build();
	}
}
