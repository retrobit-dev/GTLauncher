package dev.retrobit.gtlauncher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntBinaryOperator;

/** Pure layout for decorative pond life on flat portable worlds; never changes world data. */
final class PondLife {
    static final class Item {
        final boolean fish;
        final int seed;
        Item(boolean fish,int seed) {this.fish=fish;this.seed=seed;}
    }
    final List<Item>[] cells;
    @SuppressWarnings("unchecked")
    private PondLife(int size) {cells=(List<Item>[])new List<?>[size*size];}
    private void add(int index,boolean fish,int seed) {
        if(cells[index]==null)cells[index]=new ArrayList<>();
        cells[index].add(new Item(fish,seed));
    }
    static PondLife build(int size,IntBinaryOperator tile) {
        PondLife result=new PondLife(size);
        boolean[] seen=new boolean[size*size];
        for(int start=0;start<seen.length;start++) {
            if(seen[start] || tile.applyAsInt(start%size,start/size)!=3)continue;
            ArrayList<Integer> queue=new ArrayList<>();queue.add(start);seen[start]=true;
            for(int i=0;i<queue.size();i++) {
                int cell=queue.get(i),x=cell%size,y=cell/size;
                for(int[] offset:new int[][]{{0,-1},{1,0},{0,1},{-1,0}}) {
                    int c=x+offset[0],r=y+offset[1];
                    if(c<0||r<0||c>=size||r>=size)continue;
                    int next=r*size+c;
                    if(!seen[next] && tile.applyAsInt(c,r)==3) {seen[next]=true;queue.add(next);}
                }
            }
            boolean square=false;
            for(int cell:queue) {
                int x=cell%size,y=cell/size;
                if(x+1<size && y+1<size && tile.applyAsInt(x+1,y)==3 && tile.applyAsInt(x,y+1)==3 && tile.applyAsInt(x+1,y+1)==3){square=true;break;}
            }
            if(!square)continue;
            Collections.sort(queue);int count=queue.size()/4;
            for(int i=0;i<count*2;i++) {
                int index=(int)Math.floor((i+.2)*queue.size()/(count*2));
                result.add(queue.get(index),true,start*131+i*37+17);
            }
            for(int i=0;i<count;i++) {
                int index=(int)Math.floor((i+.6)*queue.size()/count);
                result.add(queue.get(index),false,start*131+i*37+71);
            }
        }
        return result;
    }
    private PondLife() {cells=null;}
}
