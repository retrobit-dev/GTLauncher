package dev.retrobit.gtlauncher;
import java.util.Calendar;
import org.junit.Test;
import static org.junit.Assert.*;
public class CalendarHudTest {
    @Test public void greenWeekdaysAndRedWeekends() {
        for (int day : new int[]{Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY}) assertFalse(CalendarHud.isWeekend(day));
        assertTrue(CalendarHud.isWeekend(Calendar.SATURDAY));
        assertTrue(CalendarHud.isWeekend(Calendar.SUNDAY));
    }
}
