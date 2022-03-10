package org.wingsofcarolina.quiz.domain.dao;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Record;


public class RecordDAO extends BasicDAO<Record, ObjectId> {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(RecordDAO.class);

	public RecordDAO(Datastore ds) {
		super(Record.class, ds);
	}

	public List<Record> getAllRecords() {
		List<Record> result = getDatastore().find(Record.class).order("quizId").asList();
		return result;
	}

	public Record getByQuizId(Long questionId) {
		List<Record> result = getDatastore().find(Record.class).filter("quizId = ", questionId).order("quizId").asList();
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}

	public Set<Long> getDeployedIds() {
		Set<Long> bucket = new HashSet<Long>();
		List<Record> records = getDatastore().find(Record.class).asList();

		for (Record record : records) {
			bucket.addAll(record.getQuestionIds());
		}
		
		return bucket;
	}

	public List<Record> getEarlierThan(Date sunset) {
		List<Record> result = getDatastore().find(Record.class).filter("createdDate < ", sunset).order("quizId").asList();
		return result;
	}
}
