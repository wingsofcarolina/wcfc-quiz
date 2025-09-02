package org.wingsofcarolina.quiz.responses;

import java.net.URI;

import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriBuilder;

public class ViewQuestionResponse extends AbstractResponse {
	private long questionId;
	private NewCookie cookies = null;

	public ViewQuestionResponse() {
		super();
	}

	public ViewQuestionResponse(long l) {
		super("ok");
		this.questionId = l;
	}
	
	public ViewQuestionResponse cookie(NewCookie cookie) {
		this.cookies = cookie;
		return this;
	}
	
	public Response build() {
		URI uri = UriBuilder.fromUri("/question/" + questionId).build();
		ResponseBuilder resp = Response.seeOther(uri);
		if (cookies != null) {
			resp.cookie(cookies);
		}
		return resp.build();
	}
}
