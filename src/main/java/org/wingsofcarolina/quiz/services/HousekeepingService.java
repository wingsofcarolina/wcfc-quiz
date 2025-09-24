package org.wingsofcarolina.quiz.services;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.HousekeepingTracker;
import org.wingsofcarolina.quiz.jobs.HousekeepingTasks;

/**
 * Service for performing housekeeping tasks asynchronously
 */
public class HousekeepingService {

  private static final Logger LOG = LoggerFactory.getLogger(HousekeepingService.class);

  private static HousekeepingService instance;
  private final ExecutorService executorService;

  private HousekeepingService() {
    // Create a single-threaded executor for housekeeping tasks
    this.executorService =
      Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "housekeeping-thread");
        t.setDaemon(true); // Don't prevent JVM shutdown
        return t;
      });
  }

  public static synchronized HousekeepingService getInstance() {
    if (instance == null) {
      instance = new HousekeepingService();
    }
    return instance;
  }

  /**
   * Trigger housekeeping if needed. This method returns immediately and
   * performs the housekeeping check and execution on a background thread.
   */
  public void triggerHousekeepingIfNeeded() {
    CompletableFuture
      .runAsync(this::performHousekeepingIfNeeded, executorService)
      .exceptionally(throwable -> {
        LOG.error("Error during background housekeeping", throwable);
        return null;
      });
  }

  /**
   * Check if housekeeping is needed and perform it if so.
   * This method runs on a background thread.
   */
  private void performHousekeepingIfNeeded() {
    try {
      HousekeepingTracker tracker = HousekeepingTracker.getInstance();

      if (tracker.isHousekeepingNeeded()) {
        LOG.info("Housekeeping needed, starting cleanup tasks");

        // Perform the actual housekeeping
        HousekeepingTasks tasks = new HousekeepingTasks();
        tasks.performHousekeeping();

        // Update the last run time
        try {
          tracker.updateLastRun();
          LOG.info("Housekeeping completed successfully");
        } catch (Exception saveException) {
          LOG.error(
            "Housekeeping tasks completed but failed to update tracker",
            saveException
          );
          // Don't rethrow - the housekeeping tasks were successful
        }
      } else {
        LOG.debug("Housekeeping not needed, skipping");
      }
    } catch (Exception e) {
      LOG.error("Error during housekeeping execution", e);
      // Continue running - don't let housekeeping failures break the service
    }
  }

  /**
   * Shutdown the executor service (for clean application shutdown)
   */
  public void shutdown() {
    if (executorService != null && !executorService.isShutdown()) {
      executorService.shutdown();
    }
  }
}
