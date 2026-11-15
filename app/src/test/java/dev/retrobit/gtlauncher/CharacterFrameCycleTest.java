package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Arrays;
public class CharacterFrameCycleTest {
    @Test public void loopsContinuouslyInBothDirections() {
        assertEquals(0,CharacterFrameCycle.index(0));
        assertEquals(2,CharacterFrameCycle.index(Math.PI/2));
        assertEquals(4,CharacterFrameCycle.index(Math.PI));
        assertEquals(6,CharacterFrameCycle.index(3*Math.PI/2));
        assertEquals(0,CharacterFrameCycle.index(2*Math.PI));
        assertEquals(6,CharacterFrameCycle.index(-Math.PI/2));
        assertEquals(0,CharacterFrameCycle.index(Double.NaN));
    }
    @Test public void refusesMissingFrames() {
        Object[] frames=new Object[8];
        assertEquals(-1,CharacterFrameCycle.availableIndex(frames,0));
        Arrays.fill(frames,new Object());
        assertEquals(4,CharacterFrameCycle.availableIndex(frames,Math.PI));
        frames[5]=null;
        assertEquals(-1,CharacterFrameCycle.availableIndex(frames,0));
        assertEquals(-1,CharacterFrameCycle.availableIndex(new Object[7],0));
    }
}
