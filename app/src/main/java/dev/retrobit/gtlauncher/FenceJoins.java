package dev.retrobit.gtlauncher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
/** Flat portable worlds: share one visual junction without merging fence data. */
final class FenceJoins {
    static final class Join {boolean postA=true,postB=true,joinA,joinB,linkA,linkB;}
    private static final class Entry {
        final WorldStore.SurfaceObject fence;final int end,index,axis;final float depth;
        Entry(WorldStore.SurfaceObject f,int end,int index){fence=f;this.end=end;this.index=index;axis=f.edge.equals("N")||f.edge.equals("S")?0:1;depth=f.column+f.row+((f.edge.equals("S")||f.edge.equals("E"))?.5f:-.5f);}
    }
    private static int[][] vertices(WorldStore.SurfaceObject f) {
        int c=f.column*2,r=f.row*2;
        switch(f.edge){case "N":return new int[][]{{c-1,r-1},{c+1,r-1}};case "E":return new int[][]{{c+1,r+1},{c+1,r-1}};case "S":return new int[][]{{c-1,r+1},{c+1,r+1}};case "W":return new int[][]{{c-1,r+1},{c-1,r-1}};default:throw new IllegalArgumentException("Invalid edge");}
    }
    static Map<WorldStore.SurfaceObject,Join> plan(List<WorldStore.SurfaceObject> objects) {
        return plan(objects,o->0);
    }
    static Map<WorldStore.SurfaceObject,Join> plan(List<WorldStore.SurfaceObject> objects, java.util.function.ToIntFunction<WorldStore.SurfaceObject> level) {
        Map<WorldStore.SurfaceObject,Join> out=new IdentityHashMap<>();Map<String,List<Entry>> groups=new HashMap<>();
        for(int i=0;i<objects.size();i++) {
            WorldStore.SurfaceObject f=objects.get(i);if(!f.type.equals("fence") || !f.material.startsWith("preview-"))continue;
            out.put(f,new Join());int[][] vertices=vertices(f);
            for(int e=0;e<2;e++){String key=level.applyAsInt(f)+":"+f.material+":"+vertices[e][0]+","+vertices[e][1];groups.computeIfAbsent(key,k->new ArrayList<>()).add(new Entry(f,e,i));}
        }
        for(List<Entry> group:groups.values()) {
            if(group.size()<2)continue;
            group.sort((a,b)->{int n=Float.compare(a.depth,b.depth);return n!=0?n:a.index!=b.index?Integer.compare(a.index,b.index):Integer.compare(a.end,b.end);});
            Entry owner=group.get(group.size()-1);boolean turn=false;for(Entry e:group)if(e.axis!=owner.axis)turn=true;
            for(Entry e:group){Join j=out.get(e.fence);boolean chosen=e==owner;if(e.end==0){j.linkA=true;j.postA=chosen;j.joinA=turn&&chosen;}else{j.linkB=true;j.postB=chosen;j.joinB=turn&&chosen;}}
        }
        return out;
    }
    private FenceJoins() {}
}
