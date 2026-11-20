package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class SeasonGameplayTest {
    @Test public void defaultsToDryUntilWetIsDetected() {
        assertEquals("wet", SeasonEstimate.gameplaySeason("wet"));
        assertEquals("dry", SeasonEstimate.gameplaySeason("dry"));
        assertEquals("dry", SeasonEstimate.gameplaySeason("unknown"));
        assertEquals("dry", SeasonEstimate.gameplaySeason(null));
    }
}
