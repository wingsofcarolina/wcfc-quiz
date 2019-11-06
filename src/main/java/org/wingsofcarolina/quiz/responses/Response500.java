package org.wingsofcarolina.quiz.responses;

import javax.ws.rs.core.Response;

public class Response500 extends AbstractResponse {

	public Response500() {
		super();
	}

	public Response500(String message) {
		super(message);
	}
	
	public Response build() {
		return super.build(500);
	}
}
