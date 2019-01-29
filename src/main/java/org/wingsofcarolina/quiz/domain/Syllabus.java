package org.wingsofcarolina.quiz.domain;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Syllabus {
	private static ObjectMapper mapper = new ObjectMapper();

	private static List<String> tasks = new ArrayList<String>();
	  
	@SuppressWarnings("unchecked")
	public Syllabus() {
		try {
			File file = new File("primaryTasks.json");
			tasks = mapper.readValue(file, ArrayList.class);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Record initialize(Record record) {
		Integer index = 0;
		for (String description : tasks) {
			record.addTask(index++, description);
		}
		return record;
	}

}
