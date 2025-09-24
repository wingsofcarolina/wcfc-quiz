package org.wingsofcarolina.quiz.domain.dao;

import java.util.List;
import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.dao.BasicDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.HousekeepingTracker;

public class HousekeepingTrackerDAO extends BasicDAO<HousekeepingTracker, ObjectId> {

  private static final Logger LOG = LoggerFactory.getLogger(HousekeepingTrackerDAO.class);

  public HousekeepingTrackerDAO(Datastore ds) {
    super(HousekeepingTracker.class, ds);
  }

  /**
   * Get housekeeping tracker by tracker ID
   */
  public HousekeepingTracker getByTrackerId(String trackerId) {
    try {
      List<HousekeepingTracker> trackers = getDatastore()
        .find(HousekeepingTracker.class)
        .filter("trackerId = ", trackerId)
        .asList();
      if (trackers.size() > 0) {
        return trackers.get(0);
      } else {
        return null;
      }
    } catch (Exception e) {
      // Log the error but don't fail - return null so a new tracker will be created
      LOG.warn(
        "Error querying HousekeepingTracker collection, will create new tracker: {}",
        e.getMessage()
      );
      return null;
    }
  }
}
