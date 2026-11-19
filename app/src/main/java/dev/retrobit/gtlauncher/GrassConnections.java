package dev.retrobit.gtlauncher;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/** Visual bridges between cardinal neighbours; never alters world records. */
final class GrassConnections {
    static final class Pair {
        final WorldStore.SurfaceObject a,b;
        Pair(WorldStore.SurfaceObject a,WorldStore.SurfaceObject b){this.a=a;this.b=b;}
    }
    static List<Pair> pairs(List<WorldStore.SurfaceObject> objects) {
        Map<String,WorldStore.SurfaceObject> grasses=new HashMap<>();
        for(WorldStore.SurfaceObject o:objects)if(o.type.equals("bush") && o.material.equals("wild-grass"))grasses.put(o.column+","+o.row,o);
        List<Pair> result=new ArrayList<>();
        for(WorldStore.SurfaceObject a:objects)if(a.type.equals("bush") && a.material.equals("wild-grass")) {
            WorldStore.SurfaceObject east=grasses.get((a.column+1)+","+a.row),south=grasses.get(a.column+","+(a.row+1));
            if(east!=null)result.add(new Pair(a,east));if(south!=null)result.add(new Pair(a,south));
        }
        return result;
    }
    private GrassConnections() {}
}
