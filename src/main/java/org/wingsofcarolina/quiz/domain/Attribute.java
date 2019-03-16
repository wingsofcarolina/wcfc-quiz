package org.wingsofcarolina.quiz.domain;

import java.util.Arrays;
import java.util.List;

public class Attribute {
	// General attributes
	public static String REQUIRED = "REQUIRED";

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
	public static String TIPS = "TIPS";
	public static String MISCELLANEOUS = "MISCELLANEOUS";
	public static String AIRCRAFT_RULES = "AIRCRAFT_RULES";
	public static List<String> aircraft_attributes = Arrays.asList(GENERAL, LIMITATIONS, EMERGENCY, PROCEDURES, PERFORMANCE,
			WB, SYSTEMS, SERVICING, SUPPLEMENTS, TIPS, MISCELLANEOUS, AIRCRAFT_RULES);

	// SOP attributes
	public static String SOP_I = "SOP_I";
	public static String SOP_II = "SOP_II";
	public static String SOP_III = "SOP_III";
	public static String SOP_IV = "SOP_IV";
	public static String SOP_V = "SOP_V";
	public static String SOP_VI = "SOP_VI";
	public static String SOP_VII = "SOP_VII";
	public static String FS = "FS";
	public static String STUDENT = "STUDENT";
	public static String INSTRUCTOR = "INSTRUCTOR";
	public static String PILOT = "PILOT";
	public static String ALL = "ALL";
	public static List<String> sop_attributes = Arrays.asList(SOP_I, SOP_II, SOP_III, SOP_IV, SOP_V, SOP_VI, SOP_VII, FS);
	public static List<String> level_attributes = Arrays.asList(STUDENT, INSTRUCTOR, PILOT, ALL);
}
