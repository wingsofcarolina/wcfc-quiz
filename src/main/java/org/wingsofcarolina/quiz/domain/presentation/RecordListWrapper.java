package org.wingsofcarolina.quiz.domain.presentation;

import java.util.List;

import org.wingsofcarolina.quiz.domain.Record;
import org.wingsofcarolina.quiz.domain.User;

public class RecordListWrapper {
	private User user;
	private List<Record> records;
	Integer index;
	Integer count;
	
	public RecordListWrapper(User user, List<Record> records, Integer index, Integer count) {
		super();
		this.user = user;
		this.records = records;
		this.index = index;
		this.count = count;
	}

	public RecordListWrapper(User user, List<Record> records) {
		super();
		this.user = user;
		this.records = records;
		this.index = 0;
		this.count = records.size();
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<Record> getRecords() {
		return records;
	}

	public void setRecords(List<Record> records) {
		this.records = records;
	}
	
	public Integer getIndex() {
		return index;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public Integer skip() {
		return index + count;
	}
	
	public Integer skipBack() {
		int skip = index - count;
		skip = skip < 0 ? 0 : skip;
		return skip;
	}
	
	@Override
	public String toString() {
		return "RecordWrapper [user=" + user + ", records=" + records + "]";
	}
}
