package org.wingsofcarolina.quiz.responses;

import jakarta.ws.rs.core.Response;

import org.wingsofcarolina.quiz.common.Error;

public class AbstractResponse {

	protected String message;
	
	public AbstractResponse() {
		this.message = "n/a";
	}
	
	public AbstractResponse(String message) {
		this.message = message;
	}
	
	public Response build(int code) {
		return Response.status(code).entity(new Error(code, message)).build();
	}
}
