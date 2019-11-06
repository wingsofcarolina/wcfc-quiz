package org.wingsofcarolina.quiz.responses;

import javax.ws.rs.core.Response;

public class ResponseOk extends AbstractResponse {

	private Object entity;
	
	public ResponseOk() {
		super();
	}

	public ResponseOk(Object entity) {
		super("ok");
		this.entity = entity;
	}
	
	public Response build() {
		return Response.ok().entity(entity).build();
	}
}
