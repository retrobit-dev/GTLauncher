package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class CharacterGaitTest {
    @Test public void shortAndLongRoutes() {
        for(int i=1;i<=3;i++) {assertFalse(CharacterGait.running(i,3));assertEquals(300,CharacterGait.durationMillis(i,3));}
        assertFalse(CharacterGait.running(1,4));assertTrue(CharacterGait.running(2,4));
        assertTrue(CharacterGait.running(3,4));assertFalse(CharacterGait.running(4,4));
        assertEquals(180,CharacterGait.durationMillis(2,4));
    }
    @Test public void runPoseBlendsAtWalkRunBoundaries() {
        assertEquals(0,CharacterGait.runBlend(1,4,1),0);
        assertEquals(0,CharacterGait.runBlend(2,4,0),0);
        assertEquals(1,CharacterGait.runBlend(2,4,.5f),0);
        assertEquals(1,CharacterGait.runBlend(2,4,1),0);
        assertEquals(1,CharacterGait.runBlend(3,4,0),0);
        assertEquals(0,CharacterGait.runBlend(3,4,1),0);
        assertEquals(0,CharacterGait.runBlend(4,4,0),0);
        assertEquals(0,CharacterGait.runBlend(2,3,.5f),0);
    }
    @Test public void progressAndPhaseStayContinuousAcrossSpeedChanges() {
        assertEquals(0,CharacterGait.progress(-1,300),0);
        assertEquals(.5,CharacterGait.progress(150,300),0);
        assertEquals(1,CharacterGait.progress(500,300),0);
        assertEquals(CharacterGait.phase(1,1),CharacterGait.phase(2,0),0);
        assertEquals(CharacterGait.phase(3,1),CharacterGait.phase(4,0),0);
    }
}
