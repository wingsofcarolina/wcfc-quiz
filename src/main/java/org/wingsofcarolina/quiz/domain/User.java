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
	private Integer memberNumber;
	@JsonIgnore
	private String token;
	@JsonIgnore
	private String password;
	@JsonIgnore
	private List<Privilege> privileges = new ArrayList<Privilege>();
	private Date created;
	private Date updated;
	private String phone;
	private String notes;
	private Record record;
	private List<String> students;
	private String message;

	// Default empty constructor needed for database
	public User() {}
	
	public User(String email) {
		this.userId = UUID.randomUUID().toString();
		this.email = email;
		this.created = new Date();
		this.updated = this.created;
		this.record = null; // No instruction, by default
	}
	public User(String email, String hashedPw) {
		this(email);
		this.password = hashedPw;
	}
	public String getUserId() {
		return userId;
	}
	public String getFullname() {
		return fullname;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	public Integer getMemberNumber() {
		return memberNumber;
	}
	public void setMemberNumber(Integer memberNumber) {
		this.memberNumber = memberNumber;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
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
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getNotes() {
		return notes;
	}
	public void setNotes(String notes) {
		this.notes = notes;
	}
	public List<String> getStudents() {
		return students;
	}
	public boolean hasStudents() {
		return students != null && students.size() > 0;
	}
	public void addStudent(User student) {
		if (students == null) {
			students = new ArrayList<String>();
		}
		// TODO: Make this better
		if (! students.contains(student)) {
			students.add(student.getUserId());
		}
	}
	public Record getRecord() {
		return record;
	}
	public void setRecord(Record record) {
		record.setOwnerId(userId);
		for (User instructor : record.getInstructors()) {
			instructor.addStudent(this);
		}
		this.record = record;
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
	public boolean isInstructor() {
		return privileges.contains(Privilege.INSTRUCTOR);
	}
	public boolean hasInstructor(User requester) {
		String requesterId = requester.getUserId();
		if (record != null) {
			for (User instructor : record.getInstructors()) {
				if (requesterId.equals(instructor.getUserId())) {
					return true;
				}
			}
		} 
		return false;
	}
	public String getMessage() {
		String msg = message;
		message = null;
		return msg;
	}
	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "User [userId=" + userId + ", fullname=" + fullname + ", email=" + email + ", memberNumber=" + memberNumber
				+ ", privileges=" + privileges + ", phone=" + phone + ", students=" + students + "]";
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
	
	public static List<User> getInstructors() {
		UserDAO userDao = (UserDAO) Persistence.instance().get(User.class);
		return userDao.getInstructors();
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
