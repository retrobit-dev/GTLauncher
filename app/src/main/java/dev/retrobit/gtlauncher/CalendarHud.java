package dev.retrobit.gtlauncher;

import java.util.Calendar;

/** Uses the device's local weekday, not a demo or weather time. */
final class CalendarHud {
    static boolean isWeekend(int weekday) {
        return weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY;
    }
    private CalendarHud() {}
}
