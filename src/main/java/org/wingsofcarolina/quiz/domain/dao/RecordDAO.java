package org.wingsofcarolina.quiz.domain.dao;
import java.util.List;

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

	public Question getByQuizId(Long questionId) {
		List<Question> result = getDatastore().find(Question.class).filter("quizId = ", questionId).order("quizId").asList();
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}
}
