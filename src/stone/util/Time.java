package stone.util;

import java.util.HashMap;

/**
 * A class for simple Time representation
 * 
 * @author Nelphindal
 */
public final class Time {

	private static final HashMap<Long, Long> daysSince1970 = new HashMap<>();
	private static final int[] daysOfMonth = new int[] { 31, 28, 31, 30, 31,
			30, 31, 31, 30, 31, 30, 31 };
	private static final String[] namesOfMonth = new String[] { "January",
			"February", "March", "April", "May", "June", "July", "August",
			"September", "October", "November", "December" };
	private static final String[] shortNamesOfMonth = new String[] { "Jan",
			"Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
			"Nov", "Dec" };

	/**
	 * Converts millis into a human readable time.
	 * 
	 * @param millis
	 *            a time in milliseconds since 00:00 CET 1.1.1970
	 * @return a human-readable date. Format: Mmm dd
	 */
	/*
	 * replaces java.util.Calendar
	 */
	public final static String date(long millis) {
		long secs = millis / 1000;
		long mins = secs / 60;
		long hours = mins / 60;
		long days = hours / 24;
		final long years = days / 365;
		long year = 1970 + years;
		days -= getDays(year);
		if (days < 0) {
			if (isLeap(--year)) {
				days += 366;
			} else {
				days += 365;
			}
		}
		int month = 0;
		if (days > Time.daysOfMonth[month]) {
			days -= Time.daysOfMonth[month++];
			final int daysFeb;
			if (isLeap(year)) {
				daysFeb = Time.daysOfMonth[month] + 1;
			} else {
				daysFeb = Time.daysOfMonth[month];
			}
			if (days > daysFeb) {
				days -= daysFeb;
				month++;
				while (days > Time.daysOfMonth[month]) {
					days -= Time.daysOfMonth[month++];
				}
			}
		}
		final String timeString = String.format("%02d:%02d:%02d", ++hours % 24,
				++mins % 60, ++secs % 60)
				+ " "
				+ ++days
				+ " "
				+ ++month
				+ " "
				+ year;
		// System.out.println("calculated: " + timeString + " ("
		// + getMonthName(String.valueOf(month)) + ")");
		return timeString;
	}

	/**
	 * Converts millis into a human readable time.
	 * 
	 * @param millis
	 *            a time difference in millis
	 * @return a human-readable time
	 */
	public final static String delta(long millis) {
		final long seconds = millis / 1000;
		final long minutes = seconds / 60;
		final long hours = minutes / 60;
		if (seconds == 0) {
			return millis + " millisecond" + (millis == 1 ? "" : "s");
		}
		if (minutes == 0) {
			return seconds + " second" + (seconds == 1 ? "" : "s") + " and "
					+ (millis % 1000) + " millisecond"
					+ ((millis % 1000) == 1 ? "" : "s");
		}
		if (hours == 0) {
			return minutes + " minute" + (minutes == 1 ? "" : "s") + " and "
					+ (seconds % 60) + " second"
					+ ((seconds % 60) == 1 ? "" : "s");
		}
		final long days = hours / 24;
		if (days == 0) {
			return hours + " hour" + (hours == 1 ? "" : "s") + " and "
					+ (minutes % 60) + " minute"
					+ ((minutes % 60) == 1 ? "" : "s");
		}
		if (days > 365) {
			final long years = days / 365;
			return days + " year" + (years == 1 ? "" : "s") + " and "
					+ (days % 365) + " day" + ((days % 365) == 1 ? "" : "s");
		} else {
			final long weeks = days / 7;
			if (weeks == 0) {
				return days + " day" + (days == 1 ? "" : "s") + " and "
						+ (hours % 24) + " hour"
						+ ((hours % 24) == 1 ? "" : "s");
			}
			return weeks + " week" + (weeks == 1 ? "" : "s") + " and "
					+ (days % 7) + " day" + ((days % 7) == 1 ? "" : "s");
		}
	}

	/**
	 * @param string
	 *            a number between 1 and 12
	 * @return the full name for given month
	 */
	public final static String getMonthName(final String string) {
		return Time.namesOfMonth[Integer.parseInt(string) - 1];
	}

	/**
	 * @return an array containing all month names
	 */
	public final static String[] getMonthNames() {
		final String[] namesOfMonth_ = new String[Time.namesOfMonth.length];
		System.arraycopy(Time.namesOfMonth, 0, namesOfMonth_, 0,
				namesOfMonth_.length);
		return namesOfMonth_;
	}

	/**
	 * @param string
	 *            a number between 1 and 12
	 * @return the name for given month shorted to 3 letters
	 */
	public final static String getShortMonthName(final String string) {
		return Time.shortNamesOfMonth[Integer.parseInt(string) - 1];
	}

	/**
	 * @return an array containing all shortened month names
	 */
	public final static String[] getShortMonthNames() {
		final String[] namesOfMonth_ = new String[Time.shortNamesOfMonth.length];
		System.arraycopy(Time.shortNamesOfMonth, 0, namesOfMonth_, 0,
				namesOfMonth_.length);
		return namesOfMonth_;
	}

	private final static long getDays(long year) {
		final Long entry = Time.daysSince1970.get(year);
		if (entry != null) {
			return entry;
		}
		long days = 0;
		for (long y = 1970; y < year; ++y) {
			final boolean leap = isLeap(y);
			if (leap) {
				days += 366;
			} else {
				days += 365;
			}
		}
		Time.daysSince1970.put(year, days);
		return days;
	}

	private final static boolean isLeap(long y) {
		boolean leap = false;
		if ((y % 4) == 0) {
			leap = true;
			if ((y % 100) == 0) {
				leap = false;
				if ((y % 1000) == 0) {
					leap = true;
				}
			}
		}
		return leap;
	}
}
