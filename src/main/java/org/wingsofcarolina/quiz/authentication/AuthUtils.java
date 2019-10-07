package org.wingsofcarolina.quiz.authentication;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.common.Flash;
import org.wingsofcarolina.quiz.domain.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.impl.DefaultClaims;

public class AuthUtils {
	private static final Logger LOG = LoggerFactory.getLogger(AuthUtils.class);

	// For SecretKeySpec generation
	private String algorithm = "HmacSHA512";
	private byte[] encoded = {-8, -36, 93, 58, -106, 125, -77, -120, -119, 80, -67, -58, -103,
			                  40, 49, -81, 51, -91, -19, 83, -67, 69, 22, 71, 74, -109, -125, 67,
			                  -72, -39, -11, -63, 19, -121, -85, 3, -32, -97, -21, -67, -127, 47,
			                  -46, -108, 99, -69, 36, 120, -67, 92, 113, 51, 96, 34, 67, -12, -44,
			                  -31, -117, -37, 92, -97, -100, 67};

	private SecretKeySpec key;
	private JwtParser parser;


	public AuthUtils() {
		key = new SecretKeySpec(encoded, algorithm);
		parser = Jwts.parser().setSigningKey(key);
	}
	
	public SecretKeySpec getKey() {
		return key;
	}
	
	public JwtParser getParser() {
		return parser;
	}

	public Jws<Claims> validateUser(String authString) throws AuthenticationException {
		return validateUser(authString, Privilege.USER);
	}


	public Jws<Claims> validateCookie(Cookie cookie, Privilege admin) throws AuthenticationException {
		Jws<Claims> result = null;
		
		if (cookie != null) {
			LOG.debug("Cookie value is : {}", cookie.getValue());
			result = validateUser(cookie.getValue(), admin);
		} else {
			LOG.debug("Cookie was NULL");
			throw new AuthenticationException(403, "User forbidden to perform operation");
		}
		
		return result;
	}
	
	public Jws<Claims> validateUser(String compactJws, Privilege privilege) throws AuthenticationException {
		Jws<Claims> claims = null;
		if (compactJws != null) {
			try {
				if (compactJws != null && ! compactJws.isEmpty()) {
					claims = parser.setSigningKey(key).parseClaimsJws(compactJws);
					String name = claims.getBody().getSubject();
					Date issuedAt = claims.getBody().getIssuedAt();
					String userId = (String)claims.getBody().get("userId");
					if (timedOut(issuedAt)) {
						Flash.add(Flash.Code.ERROR, "User idle too long.");
						throw new AuthenticationException(403, "User idle too long.");
					}
					LOG.debug("Last user interaction : {}", issuedAt);
					
					User user = User.getByEmail(name);
					if (user != null && userId != null) {
						if ( ! user.getUserId().contentEquals(userId)) {
							throw new AuthenticationException();
						}
						List<Privilege> privs = user.getPrivs();
						if ( ! (privs.contains(Privilege.ADMIN) || privs.contains(privilege))) {
							Flash.add(Flash.Code.ERROR, "User not authorized to perform operation.");
							throw new AuthenticationException(403, "User not authorized to perform operation");
						}
					} else {
						throw new AuthenticationException();
					}
					LOG.debug("Claims : {}", claims.getBody());
			    } else {
			        throw new AuthenticationException();
			    }
			} catch (SignatureException ex) {
		        throw new AuthenticationException();
			}
		} else {
			throw new AuthenticationException(400, "Authentication string not provided");
		}
		return claims;
	}
	
	private boolean timedOut(Date issuedAt) {
		long secs = (new Date().getTime() - issuedAt.getTime()) / 1000;
		long hours = secs / 3600;    
		return hours > 2;
	}
	
	public String generateToken(User user, Long questionId) {
		// Now generate the Java Web Token
		// https://github.com/jwtk/jjwt
		// https://stormpath.com/blog/jwt-java-create-verify
		// https://scotch.io/tutorials/the-anatomy-of-a-json-web-token
		Claims claims = new DefaultClaims();
		claims.setIssuedAt(new Date());
		claims.setSubject(user.getEmail());
		claims.put("email", user.getEmail());
		claims.put("admin", user.getPrivileges().contains(Privilege.ADMIN));
		claims.put("userId", user.getUserId());
		if (questionId != null) {
			questionId = new Long(-1);
		}
		claims.put("questionId", questionId);
		String compactJws = Jwts.builder().setClaims(claims).signWith(SignatureAlgorithm.HS512, key).compact();

		return compactJws;
	}

	public NewCookie generateCookie(User user) {
		return new NewCookie("quiz.token", generateToken(user, null), "/", "", "Quiz Login Token", -1, false);
	}

	public NewCookie generateCookie(User user, Long questionId) {
		return new NewCookie("quiz.token", generateToken(user, questionId), "/", "", "Quiz Login Token", -1, false);
	}

}
