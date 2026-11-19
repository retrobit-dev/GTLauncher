package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;

public class RampGeometryTest {
    @Test public void fourDirectionsHaveContinuousInclineAndTriangularSides() {
        for(String direction:new String[]{"N","E","S","W"})for(double width:new double[]{20,28})for(double depth:new double[]{8,14})for(double height:new double[]{4,9}) {
            RampGeometry.Shape shape=RampGeometry.create(direction,width,depth,height);assertEquals(4,shape.top.length);assertEquals(2,shape.sides.length);
            for(RampGeometry.Vertex p:shape.top){assertEquals(RampGeometry.fraction(direction,p.u,p.v),p.z,1e-9);assertEquals((p.u-p.v)*width/2,p.x,1e-9);assertEquals((p.u+p.v-1)*depth/2-p.z*height,p.y,1e-9);}
            for(RampGeometry.Vertex[] side:shape.sides){RampGeometry.Vertex a=side[0],b=side[1],c=side[2];assertEquals(0,a.z,1e-9);assertEquals(0,b.z,1e-9);assertEquals(1,c.z,1e-9);assertEquals(b.u,c.u,1e-9);assertEquals(b.v,c.v,1e-9);assertEquals(1,Math.abs(a.u-b.u)+Math.abs(a.v-b.v),1e-9);assertEquals(0,RampGeometry.fraction(direction,a.u,a.v),1e-9);assertEquals(1,RampGeometry.fraction(direction,c.u,c.v),1e-9);}
            RampGeometry.Vertex a=shape.highFace[0],b=shape.highFace[1],c=shape.highFace[2],d=shape.highFace[3];
            assertEquals(0,a.z,1e-9);assertEquals(0,b.z,1e-9);assertEquals(1,c.z,1e-9);assertEquals(1,d.z,1e-9);assertEquals(a.u,d.u,1e-9);assertEquals(a.v,d.v,1e-9);assertEquals(b.u,c.u,1e-9);assertEquals(b.v,c.v,1e-9);assertEquals(1,Math.abs(a.u-b.u)+Math.abs(a.v-b.v),1e-9);assertEquals(1,RampGeometry.fraction(direction,a.u,a.v),1e-9);assertEquals(1,RampGeometry.fraction(direction,b.u,b.v),1e-9);
            for(int i=0;i<=10;i++){double t=i/10.0,u=direction.equals("E")?t:direction.equals("W")?1-t:.5,v=direction.equals("S")?t:direction.equals("N")?1-t:.5;assertEquals(t,RampGeometry.fraction(direction,u,v),1e-9);}
        }
        assertTrue(RampGeometry.create("X",28,14,4)==null);assertTrue(RampGeometry.create("E",28,14,0)==null);assertTrue(Double.isNaN(RampGeometry.fraction("E",1.1,0)));
    }
}
