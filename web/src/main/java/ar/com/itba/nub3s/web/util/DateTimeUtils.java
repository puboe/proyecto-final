package ar.com.itba.nub3s.web.util;

import org.joda.time.DateTime;

public class DateTimeUtils {

	public static String[] parseDateTimeString(String dateTime) {
		String[] dateAndTime = dateTime.split("T");
		String[] date = dateAndTime[0].split("-");
		dateAndTime[0] = new DateTime(
			Integer.valueOf(date[0]),Integer.valueOf(date[1]),Integer.valueOf(date[2]), 0, 0).toString("dd-MM-yyyy");
		dateAndTime[1] = dateAndTime[1].substring(0, dateAndTime[1].length() - 3);
		return dateAndTime;
	}
	
}
