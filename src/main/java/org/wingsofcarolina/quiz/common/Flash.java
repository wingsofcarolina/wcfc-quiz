package org.wingsofcarolina.quiz.common;

import java.util.ArrayList;
import java.util.List;

public class Flash {
	public enum Code { SUCCESS, WARN, ERROR };
	
	private static List<Message> queue = new ArrayList<Message>();

	public static void add(Code code, String message) {
		queue.add(new Message(code, message));
	}
	
	public static Message message() {
		if (queue.size() > 0) {
			return queue.remove(0);
		} else {
			return null;
		}
	}
	
	public static class Message {
		Code code;
		String message;
		
		public Message(Code code, String message) {
			super();
			this.code = code;
			this.message = message;
		}
		public Code getCode() {
			return code;
		}
		public void setCode(Code code) {
			this.code = code;
		}
		public String getMessage() {
			return message;
		}
		public void setMessage(String message) {
			this.message = message;
		}
		public String getDivClass() {
			switch (code) {
			case SUCCESS : return "flash-success";
			case WARN : return "flash-warn";
			case ERROR : return "flash-error";
			default : return "flash";
			}
		}
		public String getDiv() {
			return "<div id=\"flash\" class=\"flash " + getDivClass() + "\">" + getMessage() + "</div>\n";

		}
	}
}
