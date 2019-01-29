package org.wingsofcarolina.quiz.domain;

import java.util.Date;

public class Task {
	private Integer index;
	private Boolean completed = false;
	private Date startedDate = null;
	private Date completionDate = null;
	private Date confirmationDate = null;
	private String description;
	private Status status = Status.TODO;
	private boolean confirmed;
	
	// Default empty constructor needed for database
	public Task() {}
	
	public Task(Integer index, String description) {
		this.index = index;
		this.description = description;
	}

	public Integer getIndex() {
		return index;
	}
	
	public Boolean getCompleted() {
		return completed;
	}

	public void setStarted() {
		this.status = Status.IN_PROGRESS;
		this.startedDate = new Date();
	}
	
	public void setCompleted() {
		this.completed = true;
		this.completionDate = new Date();
		this.status = Status.COMPLETED;
	}

	public Date getStartedDate() {
		return startedDate;
	}

	public Date getCompletionDate() {
		return completionDate;
	}

	public void setCompletionDate(Date completionDate) {
		this.completionDate = completionDate;
	}

	public Date getConfirmationDate() {
		return confirmationDate;
	}

	public void setConfirmationDate(Date confirmationDate) {
		this.confirmationDate = confirmationDate;
	}

	public void setConfirmed() {
		this.confirmed = true;
		this.confirmationDate = new Date();
		this.status = Status.CONFIRMED;
	}
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

}
