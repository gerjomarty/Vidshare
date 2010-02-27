package com.gm375.vidshare.util;

import java.util.Calendar;

public class DateHelper {
    private static final String TIME_SEP = ":";
    private static final String DATE_SEP = "/";
    
    public static String dateFormatter(Calendar cal) {
        // Formats dates like: 11:33pm 27/02/2010
        
        String hour = String.valueOf(cal.get(Calendar.HOUR));
        String minute = padWithZero(String.valueOf(cal.get(Calendar.MINUTE)));
        String ampm;
        if (cal.get(Calendar.AM_PM) == Calendar.AM) {
            ampm = "am";
        } else {
            ampm = "pm";
        }
        String day = padWithZero(String.valueOf(cal.get(Calendar.DAY_OF_MONTH)));
        String month = padWithZero(String.valueOf(cal.get(Calendar.MONTH) + 1));
        String year = String.valueOf(cal.get(Calendar.YEAR));
        
        String date = hour + TIME_SEP + minute + ampm + " " +
            day + DATE_SEP + month + DATE_SEP + year;
        return date;
    }
    
    private static String padWithZero(String s) {
        if (s.length() > 1) {
            return s;
        }
        String ret = "0" + s;
        return ret;
    }
    
}
