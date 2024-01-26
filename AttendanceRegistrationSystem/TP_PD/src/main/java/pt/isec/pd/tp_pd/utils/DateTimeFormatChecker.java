package pt.isec.pd.tp_pd.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeFormatChecker {
    public static boolean isValidDateFormat(String dateStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // Verifies format 'yyyy-MM-dd'

        try {
            LocalDate.parse(dateStr, formatter); // Try to parse the string to a date
            return true; // No exception, correct format
        } catch (DateTimeParseException e) {
            return false; // Exception, not the correct format
        }
    }

    public static boolean isValidTimeFormat(String timeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");  // Verifies format 'HH:mm:ss'

        try {
            LocalTime.parse(timeStr, formatter); // Try to parse the string to a time
            return true; // No exception, correct format
        } catch (DateTimeParseException e) {
            return false; // Exception, not the correct format
        }
    }

    public static boolean isValidDateTimeFormat(String timeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH:mm");  // Este formatter verifica o formato 'HH:mm:ss'

        try {
            LocalTime.parse(timeStr, formatter); // Try to parse the string to an hour
            return true; // No exception, correct format
        } catch (DateTimeParseException e) {
            return false; // Exception, not the correct format
        }
    }


    public static boolean isTimePeriodValid(String startHour, String checkHour, String endHour) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime earlierTime = LocalTime.parse(startHour, formatter);
        LocalTime laterTime = LocalTime.parse(endHour, formatter);
        LocalTime checkTime = LocalTime.parse(checkHour, formatter);

        return checkTime.isAfter(earlierTime) && checkTime.isBefore(laterTime);
    }

    public static boolean isDateRelevant(LocalDate date) {
        LocalDate today = LocalDate.now();
        return !date.isBefore(today);
    }
}
