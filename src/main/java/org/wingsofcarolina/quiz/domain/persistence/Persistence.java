package org.wingsofcarolina.quiz.domain.persistence;

import java.util.HashMap;
import java.util.Map;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.User;
import org.wingsofcarolina.quiz.domain.dao.UserDAO;

import com.mongodb.MongoClient;

public class Persistence {
	private static final Logger LOG = LoggerFactory.getLogger(Persistence.class);

	private Morphia morphia;
	private MongoClient mongo;
	private Datastore datastore;
	@SuppressWarnings("rawtypes")
	private Map<Class, Object> daoStore = new HashMap<Class, Object>();

	private static Persistence instance = null;
	
	public Persistence() {}

	public Persistence initialize(String mongodb) {
		if (instance == null) {
			// Connect to MongoDB via Morphia
			morphia = new Morphia();
			LOG.info("Connecting to MongoDB with '{}'", mongodb);
			mongo = new MongoClient(mongodb, 27017);
			morphia.mapPackage("com.skyegadgets.domain");
			datastore = morphia.createDatastore(mongo, "wcfc-quiz");
			
			// Create DAOs
			daoStore.put(User.class, new UserDAO(datastore));
			
			// Make this a singleton
			instance = this;
		}
		return this;
	}
	
	public static Persistence instance() {
		return instance;
	}
	
	@SuppressWarnings("rawtypes")
	public BasicDAO get(Class clazz) {
		return (BasicDAO) daoStore.get(clazz);
	}
}
