package utils;

import java.time.LocalDate;
import java.time.ZoneId;

public class DateUtil {

    /**
     * 秒级时间戳
     */
    public static int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000);
    }

    /**
     * 日期时间戳
     */
    public static short getCurrentDateTimeStamp() {
        return (short) (LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000000);
    }
}
