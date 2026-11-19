package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlantMotionVisibilityTest {
    @Test public void grassMovesInLightWindButRespectsCalmAndReducedMotion() {
        double a=PlantMotion.sway(true,4,90,0,0,26,27,"bush","wild-grass");
        double b=PlantMotion.sway(true,4,90,0,1,26,27,"bush","wild-grass");
        assertTrue(Math.abs(a-b)>.35);
        assertEquals(0,PlantMotion.sway(false,4,90,0,1,26,27,"bush","wild-grass"),0);
        assertEquals(0,PlantMotion.sway(true,0,90,0,1,26,27,"bush","wild-grass"),0);
        assertEquals(2*a,PlantMotion.sway(true,4,90,0,0,26,27,"bush","preview-bush-flower-large"),.0001);
    }
}
