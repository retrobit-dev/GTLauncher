package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
public class DayNightTest {
    private static long utc(int hour) {
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        c.clear(); c.set(2026, Calendar.NOVEMBER, 7, hour, 0, 0); return c.getTimeInMillis();
    }
    @Test public void indonesiaDayAndNightFollowSolarAltitude() {
        assertFalse(DayNight.isNight(utc(5), -6.97, 110.42)); // 12:00 WIB
        assertTrue(DayNight.isNight(utc(13), -6.97, 110.42)); // 20:00 WIB
    }
    @Test public void sameInstantDependsOnLocation() {
        assertFalse(DayNight.isNight(utc(5), -6.97, 110.42));
        assertTrue(DayNight.isNight(utc(5), 40.7, -74));
    }
}
