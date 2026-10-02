
package utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;


public class _Time {

    public static long timeDay = GetTime();

    public static long GetTime() {
        Date currentDate = new Date();
        long currentLong = currentDate.getTime();

        return currentLong;
    }

    public static long GetTimeNextDay() {
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.DATE, 1);
        Date nextDate = calendar.getTime();
        return nextDate.getTime();
    }
    
    public static String ConvertTime(Long time){
        LocalDateTime currentDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM");
        return currentDateTime.format(formatter);
    }

    public static String getTimeLeft(long timeBack) {
        long current = System.currentTimeMillis();
        long diff = timeBack - current;

        if (diff <= 0) return "Respawned";

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm left", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds left", minutes, seconds % 60);
        } else {
            return String.format("%ds left", seconds);
        }
    }

    public static String formatNumber(long number) {
        if (number >= 1_000_000_000) {
            return String.format("%.1fB", number / 1_000_000_000.0);
        } else if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
}
