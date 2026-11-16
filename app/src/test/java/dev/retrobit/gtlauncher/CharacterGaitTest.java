package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class CharacterGaitTest {
    @Test public void allRoutesUseRelaxedWalkTiming() {
        for(int total:new int[]{1,3,4,20}) for(int i=1;i<=total;i++) {
            assertFalse(CharacterGait.running(i,total));
            assertEquals(650,CharacterGait.durationMillis(i,total));
            assertEquals(0,CharacterGait.runBlend(i,total,.5f),0);
        }
    }
    @Test public void elbowsBendOnlyAsRunningBlendsIn() {
        assertEquals(0, CharacterGait.elbowRadians(1,0,true),0);
        assertEquals(0, CharacterGait.elbowRadians(-1,0,false),0);
        assertEquals(1.05f,CharacterGait.elbowRadians(0,1,true),.0001);
        assertEquals(CharacterGait.elbowRadians(1,1,true),CharacterGait.elbowRadians(-1,1,false),.0001);
        assertTrue(CharacterGait.elbowRadians(1,1,true) > CharacterGait.elbowRadians(1,1,false));
        assertEquals(CharacterGait.elbowRadians(1,1,true)/2,CharacterGait.elbowRadians(1,.5f,true),.0001);
    }
    @Test public void progressAndPhaseStayContinuousAcrossSpeedChanges() {
        assertEquals(0,CharacterGait.progress(-1,300),0);
        assertEquals(.5,CharacterGait.progress(150,300),0);
        assertEquals(1,CharacterGait.progress(500,300),0);
        assertEquals(CharacterGait.phase(1,1),CharacterGait.phase(2,0),0);
        assertEquals(CharacterGait.phase(3,1),CharacterGait.phase(4,0),0);
    }
}
