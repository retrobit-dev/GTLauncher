package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class HudWeatherTest {
    @Test public void weatherCodesUseExplicitCategories() {
        assertEquals("unknown", HudWeather.kind(-1));
        assertEquals("clear", HudWeather.kind(0));
        assertEquals("cloud", HudWeather.kind(3));
        assertEquals("cloud", HudWeather.kind(45));
        assertEquals("rain", HudWeather.kind(63));
        assertEquals("rain", HudWeather.kind(81));
        assertEquals("snow", HudWeather.kind(73));
        assertEquals("snow", HudWeather.kind(86));
        assertEquals("storm", HudWeather.kind(95));
        assertEquals("unknown", HudWeather.kind(100));
    }
}
