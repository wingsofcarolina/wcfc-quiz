package org.wingsofcarolina.quiz.domain.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.*;
import org.wingsofcarolina.quiz.domain.dao.*;

import com.mongodb.MongoClient;

public class Persistence {
	private static final Logger LOG = LoggerFactory.getLogger(Persistence.class);

	private Morphia morphia;
	private MongoClient mongo;
	private Datastore datastore;
	@SuppressWarnings("rawtypes")
	private Map<Class, Object> daoStore = new HashMap<Class, Object>();

	private static Persistence instance = null;

	public Persistence() {
	}

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
			daoStore.put(Question.class, new QuestionDAO(datastore));
			daoStore.put(Recipe.class, new RecipeDAO(datastore));
			daoStore.put(Record.class, new RecordDAO(datastore));
			daoStore.put(ExclusionGroup.class, new ExclusionGroupDAO(datastore));

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

	public AutoIncrement setID(final String key, final long setvalue) {
		AutoIncrement inc = null;
		List<AutoIncrement> autoIncrement = datastore.find(AutoIncrement.class).filter("_id = ", key).asList();
		if (autoIncrement == null || autoIncrement.size() == 0) {
			inc = new AutoIncrement(key, setvalue);
			datastore.save(inc);
		} else {
			if (autoIncrement != null && autoIncrement.get(0) != null) {
				inc = autoIncrement.get(0);
				inc.setValue(setvalue);
				datastore.save(inc);
			}
		}
		return inc;
	}
	
	public long getID(final String key, final long minimumValue) {

		// Get the given key from the auto increment entity and try to increment it.
		final Query<AutoIncrement> query = datastore.find(AutoIncrement.class).field("_id").equal(key);
		final UpdateOperations<AutoIncrement> update = datastore
				.createUpdateOperations(AutoIncrement.class).inc("value");
		AutoIncrement autoIncrement = datastore.findAndModify(query, update);

		// If none is found, we need to create one for the given key.
		if (autoIncrement == null) {
			autoIncrement = new AutoIncrement(key, minimumValue);
			datastore.save(autoIncrement);
		}
		return autoIncrement.getValue();
	}
}
