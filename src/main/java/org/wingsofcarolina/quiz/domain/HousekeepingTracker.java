package org.wingsofcarolina.quiz.domain;

import java.util.Date;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.dao.HousekeepingTrackerDAO;
import org.wingsofcarolina.quiz.domain.persistence.Persistence;

@Entity("HousekeepingTracker")
public class HousekeepingTracker {

  private static final Logger LOG = LoggerFactory.getLogger(HousekeepingTracker.class);

  public static final String SINGLETON_ID = "housekeeping";

  @Id
  private String id;

  private String trackerId;
  private Date lastRun;

  public HousekeepingTracker() {}

  public HousekeepingTracker(String trackerId) {
    this.trackerId = trackerId;
    this.lastRun = new Date();
  }

  /**
   * Get the singleton housekeeping tracker record
   */
  public static HousekeepingTracker getInstance() {
    try {
      HousekeepingTrackerDAO dao = (HousekeepingTrackerDAO) Persistence
        .instance()
        .get(HousekeepingTracker.class);
      HousekeepingTracker tracker = dao.getByTrackerId(SINGLETON_ID);
      if (tracker == null) {
        LOG.info("Creating new HousekeepingTracker instance");
        tracker = new HousekeepingTracker(SINGLETON_ID);
        try {
          tracker.save();
        } catch (Exception saveException) {
          LOG.warn(
            "Failed to save new HousekeepingTracker, will continue with in-memory instance: {}",
            saveException.getMessage()
          );
          // Continue with the in-memory tracker - it will still work for this session
        }
      }
      return tracker;
    } catch (Exception e) {
      LOG.warn(
        "Error retrieving HousekeepingTracker from database, creating temporary instance: {}",
        e.getMessage()
      );
      // Return a temporary tracker that will work for this session
      return new HousekeepingTracker(SINGLETON_ID);
    }
  }

  /**
   * Check if housekeeping is needed (more than 24 hours since last run)
   */
  public boolean isHousekeepingNeeded() {
    if (lastRun == null) {
      return true;
    }

    long currentTime = System.currentTimeMillis();
    long lastRunTime = lastRun.getTime();
    long twentyFourHoursInMs = 24 * 60 * 60 * 1000; // 24 hours in milliseconds

    return (currentTime - lastRunTime) > twentyFourHoursInMs;
  }

  /**
   * Update the last run time to now
   */
  public void updateLastRun() {
    this.lastRun = new Date();
    this.save();
  }

  // Getters and setters
  public String getTrackerId() {
    return trackerId;
  }

  public void setTrackerId(String trackerId) {
    this.trackerId = trackerId;
  }

  public Date getLastRun() {
    return lastRun;
  }

  public void setLastRun(Date lastRun) {
    this.lastRun = lastRun;
  }

  // Database operations
  @SuppressWarnings("unchecked")
  public void save() {
    Persistence.instance().get(HousekeepingTracker.class).save(this);
  }

  @SuppressWarnings("unchecked")
  public void delete() {
    Persistence.instance().get(HousekeepingTracker.class).delete(this);
  }
}
