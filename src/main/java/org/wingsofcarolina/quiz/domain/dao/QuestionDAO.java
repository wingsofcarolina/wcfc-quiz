package org.wingsofcarolina.quiz.domain.dao;
import java.util.List;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Category;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Type;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBCollection;
import com.mongodb.WriteResult;
import com.mongodb.client.model.DeleteOptions;


public class QuestionDAO extends BasicDAO<Question, ObjectId> {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(QuestionDAO.class);

	public QuestionDAO(Datastore ds) {
		super(Question.class, ds);
	}

	public List<Question> getAllValidQuestions() {
		List<Question> result = getDatastore().find(Question.class).order("questionId").asList();
		return result;
	}

	public List<Question> getAllQuestions() {
		List<Question> result = getDatastore().find(Question.class).order("questionId").asList();
		return result;
	}
	

	public List<Question> getQuestionsLimited(int skip, int count) {
		List<Question> result = getDatastore().find(Question.class).order("questionId").asList(new FindOptions().skip(skip).limit(10));
		return result;
	}
	
	public Question getByQuestionId(Long questionId) {
		List<Question> result = getDatastore().find(Question.class).filter("questionId = ", questionId).order("questionId").asList();
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}
	
	public List<Question> getByType(Type type) {
		List<Question> result = getDatastore().find(Question.class).filter("type = ", type).order("questionid").asList();
		return result;
	}
	
	public List<Question> getSelectedWithAll(Category category, List<String> attributes) {
		if (attributes == null || attributes.isEmpty()) {
			return getSelected(category);
		} else {
			Query<Question> query = getDatastore().createQuery(Question.class).disableValidation();
	
			query.filter("category = ", category);
			if (attributes.size() == 1) {
				query.filter("attributes = ", attributes.get(0));
			} else {
				query.filter("attributes = ", attributes);
			}
			query.get();
			
			List<Question> result = query.order("questionid").asList();
			return result;
		}
	}

	public List<Question> getSelected(Category category) {
		Query<Question> query = getDatastore().createQuery(Question.class).disableValidation();
		query.filter("category = ", category);
		return query.order("questionid").asList();
	}

	public void drop() {
		getDatastore().getCollection(Question.class).drop();
	}
}
