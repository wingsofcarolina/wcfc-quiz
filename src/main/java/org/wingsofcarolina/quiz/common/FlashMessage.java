package org.wingsofcarolina.quiz.common;

public class FlashMessage {
	private static String flash;

	public static void set(String message) {
		flash = message;
	}
	
	public static String message() {
		String tmp = flash;
		flash = null;
		return tmp;
	}
}
