package com.vega.service.api.common;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;

import org.codehaus.jackson.map.ObjectMapper;

public class DateUtil {

    static ObjectMapper jsonObjectMapper = new ObjectMapper();
    static private SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");
    static private SimpleDateFormat timeFormat = new SimpleDateFormat("HHmmss");
    static Logger logger = Logger.getLogger(DateUtil.class);
    /**
     * Ham convert tu String sang Date
     *
     * @param str
     * @return
     * @throws ParseException
     */
    static public Date parseDate(String str) throws ParseException {
        return dateFormat.parse(str);
    }

    /**
     * Ham format date sang String
     *
     * @param date
     * @return
     */
    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }

    public static String formatTime(Date date) {
        return timeFormat.format(date);
    }

    public static String getDayNow() {
        // Print dates of the current week starting on Monday
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.format(new Date());
    }

    public static String getWeek() {
        StringBuilder weekStr = new StringBuilder();
        // Get calendar set to current date and time
        Calendar c = Calendar.getInstance();

        // Set the calendar to monday of the current week
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        // add monday
        weekStr.append(df.format(c.getTime()));
        // add sunday
        c.add(Calendar.DATE, 6);
        weekStr.append("_" + df.format(c.getTime()));
        return weekStr.toString();
    }

    public static Date[] getLimitTimeOfWeek() throws ParseException {
        Date[] dates = new Date[2];
        // Get calendar set to current date and time
        Calendar c = Calendar.getInstance();

        // Set the calendar to monday of the current week
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        // Print dates of the current week starting on Monday
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date time = c.getTime();
        String startStr = df.format(time) + " 00:00:00";
        dates[0] = parseDate(startStr);
        c.add(Calendar.DATE, 7);
        time = c.getTime();
        String endStr = df.format(time) + " 00:00:00";
        dates[1] = parseDate(endStr);
        return dates;
    }

    public static Timestamp getTimeStamp(Date date) {
        return new Timestamp(date.getTime());
    }

    public static Calendar string2Calendar(String date) {
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(dateFormat.parse(date));
        } catch (ParseException ex) {
           logger.error(ex);
        }
        return cal;
    }
    public static int daysBetween(Timestamp startDate, Timestamp endDate) {

        if (startDate != null && endDate != null) {
            long MILLI_SECONDS_IN_A_DAY = 1000 * 60 * 60 * 24;
            return (int) ((endDate.getTime() - startDate.getTime()) / MILLI_SECONDS_IN_A_DAY);
        }

        return 0;
    }
}
