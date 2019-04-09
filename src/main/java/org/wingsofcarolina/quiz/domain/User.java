package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.domain.dao.UserDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;

public class User {
    @Id
	@JsonIgnore
    private ObjectId id;
	private String userId;
	private String fullname;
	private String email;
	@JsonIgnore
	private String token;
	@JsonIgnore
	private String password;
	@JsonIgnore
	private List<Privilege> privileges = new ArrayList<Privilege>();
	private Date created;

	// Default empty constructor needed for database
	public User() {}
	
	public User(String email) {
		this.userId = UUID.randomUUID().toString();
		this.email = email;
		this.created = new Date();
	}
	public User(String email, String hashedPw) {
		this(email);
		this.password = hashedPw;
	}
	public String getUserId() {
		return userId;
	}
	public String getEmail() {
		return email;
	}
	public String getFullname() {
		return fullname;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Object getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public List<Privilege> getPrivs() {
		return privileges;
	}
	public boolean isAdmin() {
		return privileges.contains(Privilege.ADMIN);
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public List<Privilege> getPrivileges() {
		return privileges;
	}
	public void setPrivileges(List<Privilege> privileges) {
		this.privileges = privileges;
	}
	public void addPriv(Privilege priv) {
		privileges.add(priv);
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", fullname=" + fullname + ", email=" + email + ", privileges=" + privileges
				+ ", created=" + created + "]";
	}

	/*
	 * Database Management Functionality
	 */
	public void logout() {
		this.setToken(null);
		this.save();
	}

	public static User getWithClaims(Jws<Claims> claims) {
		User user = null;
		String userId = (String)claims.getBody().get("userId");
		if (userId != null) {
			user = getByUserId(userId);
		}
		return user;
	}

	public static List<User> getAllUsers() {
		UserDAO userDao = (UserDAO) Persistence.instance().get(User.class);
		return userDao.getAllUsers();
	}
	
	public static User getByUserId(String userId) {
		UserDAO userDao = (UserDAO) Persistence.instance().get(User.class);
		return userDao.getByUserId(userId);
	}
	
	public static User getByEmail(String email) {
		UserDAO userDao = (UserDAO) Persistence.instance().get(User.class);
		return userDao.getByEmail(email);
	}
	
	@SuppressWarnings("unchecked")
	public void save() {
		Persistence.instance().get(User.class).save(this);
	}
	
	@SuppressWarnings("unchecked")
	public void delete() {
		Persistence.instance().get(User.class).delete(this);
	}

}
