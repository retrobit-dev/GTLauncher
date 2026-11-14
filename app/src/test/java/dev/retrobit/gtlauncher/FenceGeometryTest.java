package dev.retrobit.gtlauncher;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class FenceGeometryTest {
    private FenceGeometry.Bounds bounds(FenceGeometry.Edge edge, java.util.List<FenceGeometry.Edge> all) {
        float x=200+(edge.column-edge.row)*14f,y=80+(edge.column+edge.row)*11.833311f/2;
        return FenceGeometry.bounds(edge,all,x,y,28f,11.833311f);
    }
    @Test public void allEdgesJoinSameAndOppositeOwnedNeighbors() {
        for(String side:new String[]{"N","E","S","W"})for(boolean opposite:new boolean[]{false,true}) {
            FenceGeometry.Edge a=new FenceGeometry.Edge(4,4,side);
            boolean along=side.equals("N")||side.equals("S");
            int c=4+(along?1:0),r=4+(along?0:-1);
            String other=side;
            if(opposite){c+=side.equals("E")?1:side.equals("W")?-1:0;r+=side.equals("S")?1:side.equals("N")?-1:0;other=side.equals("N")?"S":side.equals("S")?"N":side.equals("E")?"W":"E";}
            FenceGeometry.Edge b=new FenceGeometry.Edge(c,r,other);
            java.util.List<FenceGeometry.Edge> both=Arrays.asList(a,b);
            FenceGeometry.Bounds ga=bounds(a,both),gb=bounds(b,both);
            assertEquals(side,ga.secondX,gb.firstX,.0001f);assertEquals(side,ga.secondY,gb.firstY,.0001f);
            FenceGeometry.Bounds alone=bounds(a,Collections.singletonList(a));
            assertTrue(side,Math.abs(alone.secondX-ga.secondX)>0.1f);
        }
    }
}
