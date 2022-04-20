package org.wingsofcarolina.quiz.jobs;

import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.knowm.sundial.Job;
import org.knowm.sundial.annotations.CronTrigger;
import org.knowm.sundial.exceptions.JobInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wingsofcarolina.quiz.domain.Question;
import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.resources.Quiz;

//@CronTrigger(cron = "0/100 * * * * ?")  // Fire every minute, for testing
@CronTrigger(cron = "0 0 4 * * ?")  // Fire 4am every day
public class Housekeeping extends Job {
	private static final Logger LOG = LoggerFactory.getLogger(Housekeeping.class);

	SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
	
	@Override
	public void doRun() throws JobInterruptException {
		LOG.info("Housekeeping triggered .....");
		
		// Expunge all ancient/expired quiz records
		expunge();
		
		// Clean up questions no longer needed (i.e. not deployed, and "deleted" or "superseded")
		takeOutTheTrash();
		
		LOG.info("Housekeeping completed.");
	}

	// Weed out expired quiz records
	private void expunge() {
		LOG.info("Checking for expired quiz records .....");
		
		// First get the sunset time
		Date sunset = Date.from(OffsetDateTime.now(ZoneOffset.UTC).minusMonths(Quiz.MONTHS_TO_LIVE + 1).toInstant());

		// Gather all the records recorded earlier than right now
		List<Record> records = Record.getEarlierThan(sunset);
		
		// For all too-old records, delete them
		for (Record record : records) {
			String created = dateFormatGmt.format(record.getCreatedDate());
			LOG.info("Expunged expired record for quiz : {} : {} : {}", record.getQuizId(), record.getQuizName(), created);
			record.delete();
		}
	}
	
	private void takeOutTheTrash() {
		LOG.info("Checking for expired trashed questions .....");

		List<Question> questions = Question.getAllQuestions();
		
		for (Question question : questions) {
			if ( ! question.isDeployed()) {
				if (question.isSuperseded() || question.isDeleted()) {
					LOG.info("Removing trash question {}, as it is marked as {}", question.getQuestionId(), removalReason(question));
					question.delete();
				}
			}
		}
	}
	
	private String removalReason(Question question) {
		if (question.isSuperseded() && question.isDeleted())
			return "SUPERSEDED & DELETED";
		else {
			if (question.isSuperseded())
				return "SUPERSEDED";
			else
				return "DELETED";
		}
	}
}
