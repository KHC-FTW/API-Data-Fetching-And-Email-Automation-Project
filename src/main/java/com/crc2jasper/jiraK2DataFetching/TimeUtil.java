package com.crc2jasper.jiraK2DataFetching;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class TimeUtil {
    private static final DateTimeFormatter defaultDateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter dateFormatterType1 = DateTimeFormatter.ofPattern("dd-MMM-yyyy (EEEE)", Locale.ENGLISH);
    private static final DateTimeFormatter dateFormatterType2 = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);

    public static String dateDayOfWeekFormatter(String inputDate){
        LocalDate date = LocalDate.parse(inputDate, defaultDateFormatter);
        return date.format(dateFormatterType1);
    }

    public static boolean checkIsToday(String inputDate){
        return LocalDate.parse(inputDate, defaultDateFormatter).equals(LocalDate.now());
    }

    public static String convertToDefaultDateFormat(String inputDate){
        LocalDate date = LocalDate.parse(inputDate, dateFormatterType2);
        return date.format(defaultDateFormatter);
    }

    public static boolean isAfter(String originalDate, String newDate){
        return LocalDate.parse(newDate, defaultDateFormatter).isAfter(LocalDate.parse(originalDate, defaultDateFormatter));
    }

}
