package org.wingsofcarolina.quiz.authentication;

public class AuthenticationException extends Throwable {
	static final long serialVersionUID = 1L;
	
    private int code;
    
    public AuthenticationException() {
        this(401);
    }
    public AuthenticationException(int code) {
        this(code, "User not authenticated, or authorized");
    }
    public AuthenticationException(int code, String message) {
        this(code, message, null);
    }
    public AuthenticationException(int code, String message, Throwable throwable) {
        super(message, throwable);
        this.code = code;
    }
    public int getCode() {
        return code;
    }
}