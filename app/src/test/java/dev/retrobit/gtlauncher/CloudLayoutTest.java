package dev.retrobit.gtlauncher;
import org.junit.Test;
import static org.junit.Assert.*;
public class CloudLayoutTest {
    @Test public void weatherControlsCounts() {
        for(int cover=0;cover<=100;cover++) {
            for(int code:new int[]{0,1})assertTrue(CloudLayout.count(cover,code)>=1 && CloudLayout.count(cover,code)<=3);
            for(int code:new int[]{2,3,45,48,51,63,80})assertTrue(CloudLayout.count(cover,code)<=6);
            for(int code:new int[]{95,96,99})assertTrue(CloudLayout.count(cover,code)>=7 && CloudLayout.count(cover,code)<=10);
        }
        assertEquals(7,CloudLayout.count(0,95));assertEquals(10,CloudLayout.count(100,95));
    }
    @Test public void sizeAndDistributionFollowTheWorld() {
        for(int grid:new int[]{9,20,25,50,100})for(int count:new int[]{1,3,6,10}) {
            float cell=CloudLayout.cellSize(28,14,28,14,grid,count);assertTrue(cell>=6);
            if(grid==50 && count==3)assertTrue(cell>20);
            boolean left=false,right=false,top=false,bottom=false;
            for(int i=0;i<count;i++){double[] p=CloudLayout.seed(i,count,grid,.5,.5);assertTrue(p[0]>=0 && p[0]<grid && p[1]>=0 && p[1]<grid);left|=p[0]<grid*.25;right|=p[0]>grid*.75;top|=p[1]<grid*.25;bottom|=p[1]>grid*.70;}
            if(count>=3)assertTrue(left && right && top && bottom);
        }
    }
    @Test public void shadowsAreSofterInOvercastWeather() {
        assertEquals(1.5f,CloudLayout.shadowBlur(5,0),0);assertEquals(3f,CloudLayout.shadowBlur(96,3),0);assertEquals(3f,CloudLayout.shadowBlur(95,63),0);assertEquals(3f,CloudLayout.shadowBlur(100,95),0);
    }
    @Test public void fadeInOutAndReducedMotion() {
        assertEquals(1,CloudLayout.shadowFade(0,true),1e-9);assertEquals(.8,CloudLayout.shadowFade(6,true),1e-9);assertEquals(.6,CloudLayout.shadowFade(12,true),1e-9);assertEquals(.8,CloudLayout.shadowFade(18,true),1e-9);assertEquals(1,CloudLayout.shadowFade(12,false),1e-9);
    }
}
