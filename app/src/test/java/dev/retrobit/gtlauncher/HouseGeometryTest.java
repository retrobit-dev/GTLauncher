package dev.retrobit.gtlauncher;

import org.junit.Test;
import static org.junit.Assert.*;

public class HouseGeometryTest {
    @Test public void alignedFoundationAndInverse() {
        for(float height:new float[]{9.58f,11.83f,14f,17.24f}) for(float scale:new float[]{.5f,1f,1.5f}) {
            HouseGeometry g=new HouseGeometry(28,height,scale);
            float[] left=g.project(HouseGeometry.BASE_LEFT,HouseGeometry.BASE_LEFT_Y);
            float[] right=g.project(HouseGeometry.BASE_RIGHT,HouseGeometry.BASE_RIGHT_Y);
            assertEquals(-42*scale,left[0],.0001f);assertEquals(42*scale,right[0],.0001f);
            assertEquals(-1.5f*height*scale,left[1],.0001f);assertEquals(left[1],right[1],.0001f);
            for(float[] point:new float[][]{{100,200},{200,444},{350,405},{500,20},{256,482}}) {
                float[] p=g.project(point[0],point[1]);float[] q=g.unproject(p[0],p[1]);
                assertEquals(point[0],q[0],.001f);assertEquals(point[1],q[1],.001f);
                for(boolean mirror:new boolean[]{false,true}) {
                    float[] bounds=g.bounds(mirror);float x=p[0]*(mirror?-1:1);
                    assertTrue(x>=bounds[0]-.001f && x<=bounds[2]+.001f && p[1]>=bounds[1]-.001f && p[1]<=bounds[3]+.001f);
                }
            }
        }
    }
}
