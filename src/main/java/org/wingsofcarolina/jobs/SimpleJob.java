package org.wingsofcarolina.jobs;

import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.CronTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CronTrigger(cron = "0/10 * * * * ?")
public class SimpleJob extends Job {
	private static final Logger LOG = LoggerFactory.getLogger(SimpleJob.class);

	@Override
	public void doRun() throws JobInterruptException {
		LOG.info("SimpleJob triggered ..... do some work!");
	}

}
