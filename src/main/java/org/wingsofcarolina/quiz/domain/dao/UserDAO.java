package org.wingsofcarolina.quiz.domain.dao;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.authentication.Privilege;
import org.wingsofcarolina.quiz.domain.User;


public class UserDAO extends BasicDAO<User, ObjectId> {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(UserDAO.class);

	public UserDAO(Datastore ds) {
		super(User.class, ds);
	}

	public List<User> getAllUsers() {
		List<User> result = getDatastore().find(User.class).order("email").asList();
		return result;
	}
	

	public User getByUserId(String userId) {
		List<User> result = getDatastore().find(User.class).filter("userId = ", userId).order("userId").asList();
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}
	
	public User getByEmail(String email) {
		List<User> result = getDatastore().find(User.class).filter("email = ", email).order("email").asList();
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}
}
