package org.wingsofcarolina.quiz.responses;

import javax.ws.rs.core.Response;

public class Response400 extends AbstractResponse {

	public Response400() {
		super();
	}
	
	public Response400(String message) {
		super(message);
	}
	
	public Response build() {
		return super.build(400);
	}
}
