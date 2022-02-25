package org.wingsofcarolina.quiz.domain.dao;
import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.ExclusionGroup;
import org.wingsofcarolina.quiz.domain.Record;


public class ExclusionGroupDAO extends BasicDAO<Record, ObjectId> {
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(ExclusionGroupDAO.class);

	public ExclusionGroupDAO(Datastore ds) {
		super(Record.class, ds);
	}
	
	public List<ExclusionGroup> getAllGroups() {
		List<ExclusionGroup> result = getDatastore().find(ExclusionGroup.class).order("name").asList();
		return result;
	}
	
	public ExclusionGroup getByGroupId(Long questionId) {
		List<ExclusionGroup> result = getDatastore().find(ExclusionGroup.class).filter("groupId = ", questionId).order("groupId").asList();
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}
	
	public ExclusionGroup getByName(String name) {
		List<ExclusionGroup> result = getDatastore().find(ExclusionGroup.class).filter("name = ", name).order("name").asList();
		if (result.size() > 0) {
			return result.get(0);
		} else {
			return null;
		}
	}

}
