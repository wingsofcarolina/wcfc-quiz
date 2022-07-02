package org.wingsofcarolina.quiz.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Attribute {
	// General attributes
	public static String TEST = "TEST";		// Denotes a test question, not to be used in production

	// Category attributes
	public static String FAR = "FAR";
	public static String SOP = "SOP";
	public static String C152 = "C152";
	public static String PA28 = "PA28";
	public static String C172 = "C172";
	public static String M20J = "M20J";

	// Difficulty attributes
	public static String EASY = "EASY";
	public static String MEDIUM = "MEDIUM";
	public static String HARD = "HARD";
	public static List<String> difficulty_attributes = Arrays.asList(EASY, MEDIUM, HARD);

	// General knowledge attributes
	public static String WEATHER = "WEATHER";
	public static List<String> knowledge_attributes = Arrays.asList(WEATHER);

	// Aircraft attributes
	public static String GENERAL = "GENERAL";
	public static String LIMITATIONS = "LIMITATIONS";
	public static String EMERGENCY = "EMERGENCY";
	public static String PROCEDURES = "PROCEDURES";
	public static String PERFORMANCE = "PERFORMANCE";
	public static String WB = "WB";
	public static String SYSTEMS = "SYSTEMS";
	public static String SERVICING = "SERVICING";
	public static String SUPPLEMENTS = "SUPPLEMENTS";
	public static String MISCELLANEOUS = "MISCELLANEOUS";
	public static String AIRCRAFT_RULES = "AIRCRAFT_RULES";
	public static String AVIONICS = "AVIONICS";
	public static String IFR = "IFR";
	public static String LICENSED_PILOT = "LICENSED_PILOT";
	public static List<String> aircraft_attributes = Arrays.asList(C152, PA28, C172, M20J, GENERAL, LIMITATIONS, EMERGENCY, PROCEDURES, PERFORMANCE,
			WB, SYSTEMS, SERVICING, SUPPLEMENTS, MISCELLANEOUS, AVIONICS, IFR, LICENSED_PILOT, AIRCRAFT_RULES);

	// SOP/FAR attributes
	public static String SOP_I = "SOP_I";
	public static String SOP_II = "SOP_II";
	public static String SOP_III = "SOP_III";
	public static String SOP_IV = "SOP_IV";
	public static String SOP_V = "SOP_V";
	public static String SOP_VI = "SOP_VI";
	public static String SOP_VII = "SOP_VII";
	public static String SOP_VIII = "SOP_VIII";
	public static String SOP_IX = "SOP_IX";
	public static String FS = "FS";
	public static String STUDENT = "STUDENT";
	public static String INSTRUCTOR = "INSTRUCTOR";
	public static String PILOT = "PILOT";
	public static String ANY = "ANY";
	public static List<String> sop_attributes = Arrays.asList(FAR, SOP, SOP_I, SOP_II, SOP_III, SOP_IV, SOP_V, SOP_VI, SOP_VII, SOP_VIII, SOP_IX, FS);
	public static List<String> level_attributes = Arrays.asList(STUDENT, INSTRUCTOR, PILOT);
	
	public static List<String> attributes(String category) {
		switch (category.toLowerCase()) {
		case "knowledge" :
			return knowledgeAttributes();
		case "regulations" :
		case "far" :
		case "sop" :
			return sopAttributes();
		case "aircraft" :
		case "c152" :
		case "c172" :
		case "pa28" :
		case "m20j" :
			return aircraftAttributes();
		case "difficulty" : return difficultyAttributes();
		default : return null;
		}
	}

	private static List<String> difficultyAttributes() {
		return difficulty_attributes;
	}

	private static List<String> aircraftAttributes() {
		List<String> attributeList = new ArrayList<String>();
		attributeList.addAll(aircraft_attributes);
		attributeList.addAll(level_attributes);
		return attributeList;
	}

	private static List<String> sopAttributes() {
		List<String> attributeList = new ArrayList<String>();
		attributeList.addAll(sop_attributes);
		attributeList.addAll(level_attributes);
		return attributeList;
	}
	
	private static List<String> knowledgeAttributes() {
		List<String> attributeList = new ArrayList<String>();
		attributeList.addAll(knowledge_attributes);
		attributeList.addAll(level_attributes);
		return attributeList;
	}
}
