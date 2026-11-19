package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/** Strict, lossless reader for the preview's portable 20x20, 25x25, 50x50 and 100x100 formats.
 */
final class PreviewWorld20 {
    final int size, version;
    private final String json;
    final String id;
    private final BlockTerrain terrain;

    private PreviewWorld20(String json, String id, int size, int version, BlockTerrain terrain) {
        this.json = json; this.id = id; this.size = size; this.version = version; this.terrain = terrain;
    }
    int tile(int column, int row) {
        if (column < 0 || row < 0 || column >= size || row >= size) throw new IndexOutOfBoundsException();
        return terrain.top(column,row);
    }
    boolean canStand(int column, int row) {
        if (column < 0 || row < 0 || column >= size || row >= size) return false;
        int tile = tile(column, row); return tile != 0;
    }
    boolean canStep(int fromColumn,int fromRow,int toColumn,int toRow) {
        return terrain.canStep(fromColumn,fromRow,toColumn,toRow);
    }
    String toJson() { return json; }
    private static void keys(JSONObject object, String... allowed) throws JSONException {
        Set<String> names = new HashSet<>(Arrays.asList(allowed));
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) { String key = keys.next(); if (!names.contains(key)) throw new JSONException("Unsupported field: " + key); }
    }
    private static String string(JSONObject o, String key) throws JSONException {
        Object value = o.get(key); if (!(value instanceof String)) throw new JSONException("Invalid " + key); return (String) value;
    }
    private static int integer(JSONObject o, String key, int min, int max) throws JSONException {
        Object value = o.get(key);
        if (!(value instanceof Number)) throw new JSONException("Invalid " + key);
        double n = ((Number)value).doubleValue();
        if (!Double.isFinite(n) || n != Math.rint(n) || n < min || n > max) throw new JSONException("Invalid " + key);
        return (int)n;
    }
    private static double scale(JSONObject o, String key) throws JSONException {
        Object value = o.get(key);
        if (!(value instanceof Number)) throw new JSONException("Invalid " + key);
        double n = ((Number)value).doubleValue();
        if (!Double.isFinite(n) || n < .5 || n > 1.5) throw new JSONException("Invalid " + key);
        return n;
    }
    private static boolean bool(JSONObject o, String key) throws JSONException {
        Object value = o.get(key); if (!(value instanceof Boolean)) throw new JSONException("Invalid " + key); return (Boolean)value;
    }
    private static void facing(JSONObject o, String key) throws JSONException {
        if (!Arrays.asList("N", "E", "S", "W").contains(string(o, key))) throw new JSONException("Invalid " + key);
    }
    static PreviewWorld20 parse(String input) throws JSONException {
        JSONObject root = new JSONObject(input);
        keys(root, "format", "version", "world");
        if (!"gtlauncher-preview-world".equals(string(root,"format"))) throw new JSONException("Unsupported format");
        int version = integer(root,"version",1,Integer.MAX_VALUE);
        if (version > 3 && version != 7 && version != 8 && version != 9 && version != 10 && version != 11 && version != 12 && version != 13) throw new JSONException("Unsupported world version");
        JSONObject w = root.getJSONObject("world");
        if(version>=7 && version<13 && !w.has("orangeTrees")) throw new JSONException("Unsupported world version");
        // Flat conifer-only worlds are supported. Stacks and other tree kinds remain rejected.
        keys(w,"id","name","description","color","showCharacter","characterColumn","characterRow","characterScale","characterFacing","house","gridSize","tiles","orangeTrees","fences");
        if(version<7 && w.has("orangeTrees")) throw new JSONException("Unsupported field: orangeTrees");
        String id=string(w,"id"), name=string(w,"name"), description=string(w,"description"), color=string(w,"color");
        if (!id.matches("[A-Za-z0-9_-]{1,128}") || id.equals("base") || name.trim().isEmpty() || name.length()>48
                || description.length()>160 || !color.matches("#[0-9a-fA-F]{6}")) throw new JSONException("Invalid world fields");
        int size=integer(w,"gridSize",20,100); if (size != 20 && size != 25 && size != 50 && size != 100) throw new JSONException("Invalid grid");
        JSONArray source=w.getJSONArray("tiles"); if (source.length()!=size*size) throw new JSONException("Invalid tiles");
        int[] tiles=new int[size*size];
        for (int i=0;i<tiles.length;i++) {
            Object v=source.get(i);
            if (!(v instanceof Number)) throw new JSONException("Invalid tile");
            double n=((Number)v).doubleValue();
            if (!Double.isFinite(n) || n!=Math.rint(n) || n<0 || n>(version==1?1:version==2?5:6)) throw new JSONException("Invalid tile");
            tiles[i]=(int)n;
        }
        bool(w,"showCharacter"); int c=integer(w,"characterColumn",0,size-1), r=integer(w,"characterRow",0,size-1);
        scale(w,"characterScale"); facing(w,"characterFacing");
        if (tiles[r*size+c]==0) throw new JSONException("Character on empty tile");
        Object house=w.get("house");
        if (house != JSONObject.NULL) {
            if (!(house instanceof JSONObject)) throw new JSONException("Invalid house");
            JSONObject h=(JSONObject)house; keys(h,"column","row","scale","facing","visible");
            int hc=integer(h,"column",0,size-3), hr=integer(h,"row",0,size-3);
            if (h.has("scale")) scale(h,"scale"); if (h.has("facing")) facing(h,"facing"); if (h.has("visible")) bool(h,"visible");
            for (int y=hr;y<hr+3;y++) for (int x=hc;x<hc+3;x++) if (tiles[y*size+x]==0) throw new JSONException("House on empty tile");
        }
        if(version>=7 && w.has("orangeTrees")) {
            Object treeValue=w.get("orangeTrees");
            if(!(treeValue instanceof JSONArray)) throw new JSONException("Unsupported object");
            JSONArray trees=(JSONArray)treeValue;
            if(trees.length()>size*size) throw new JSONException("Unsupported object");
            boolean[] occupied=new boolean[size*size];
            java.util.Set<String> plantIds=new java.util.HashSet<>();
            boolean show=bool(w,"showCharacter");
            for(int i=0;i<trees.length();i++) {
                Object value=trees.get(i);if(!(value instanceof JSONObject)) throw new JSONException("Unsupported object");
                JSONObject tree=(JSONObject)value;keys(tree,"column","row","kind","size","facing","variant","plantId","plantedAt");
                String kind=string(tree,"kind");
                if(!"conifer".equals(kind) && !(version>=9 && "wild-grass".equals(kind)) && !(version>=10 && "bush".equals(kind))) throw new JSONException("Unsupported object");
                int footprint=2;
                if(tree.has("size")) {if(version<8)throw new JSONException("Unsupported object");footprint=integer(tree,"size",1,(version>=11 && "bush".equals(kind))?2:1);}
                if(("wild-grass".equals(kind) || "bush".equals(kind)) && !tree.has("size")) throw new JSONException("Unsupported object");
                if("wild-grass".equals(kind) && footprint!=1)throw new JSONException("Unsupported object");
                if(tree.has("variant") && (version<11 || !"bush".equals(kind) || !Arrays.asList("flower","berry").contains(string(tree,"variant"))))throw new JSONException("Unsupported object");
                if(tree.has("plantId") || tree.has("plantedAt")) {
                    if(version<12 || !"bush".equals(kind) || !tree.has("variant") || !tree.has("plantId") || !tree.has("plantedAt"))throw new JSONException("Unsupported object");
                    String plantId=string(tree,"plantId");if(!plantId.matches("[A-Za-z0-9_-]{1,80}") || !plantIds.add(plantId))throw new JSONException("Invalid plant identity");
                    Object valueTime=tree.get("plantedAt");if(!(valueTime instanceof Number))throw new JSONException("Invalid plant time");
                    double time=((Number)valueTime).doubleValue();if(!Double.isFinite(time) || time<0 || time>253402300799000d || time!=Math.floor(time))throw new JSONException("Invalid plant time");
                }
                if(tree.has("facing"))facing(tree,"facing");
                int tc=integer(tree,"column",0,size-footprint),tr=integer(tree,"row",0,size-footprint);
                for(int y=tr;y<tr+footprint;y++)for(int x=tc;x<tc+footprint;x++) {
                    int index=y*size+x;
                    if(occupied[index] || tiles[index]==0 || tiles[index]==3 || (show && x==c && y==r && "conifer".equals(kind)))throw new JSONException("Object collision");
                    if(house instanceof JSONObject) {JSONObject h=(JSONObject)house;int hc=h.getInt("column"),hr=h.getInt("row");
                        if(x>=hc && x<hc+3 && y>=hr && y<hr+3)throw new JSONException("Object collision");}
                    occupied[index]=true;
                }
            }
        }
        if(w.has("fences")) {
            if(version<13 || !(w.get("fences") instanceof JSONArray))throw new JSONException("Unsupported fences");
            JSONArray fences=w.getJSONArray("fences");if(fences.length()>size*size*2+size*2)throw new JSONException("Too many fences");
            java.util.Set<String> seen=new java.util.HashSet<>();
            for(int i=0;i<fences.length();i++) {
                JSONObject f=fences.getJSONObject(i);keys(f,"column","row","edge","material");
                int x=integer(f,"column",0,size-1),y=integer(f,"row",0,size-1);
                String edge=string(f,"edge"),material=string(f,"material");
                if(!Arrays.asList("N","E","S","W").contains(edge) || !Arrays.asList("wood","stone").contains(material) || tiles[y*size+x]==0 || tiles[y*size+x]==3)throw new JSONException("Invalid fence");
                if(house instanceof JSONObject) {JSONObject h=(JSONObject)house;int hc=h.getInt("column"),hr=h.getInt("row");if(x>=hc && x<hc+3 && y>=hr && y<hr+3)throw new JSONException("Fence overlaps house");}
                String key=(edge.equals("W")?x-1:x)+","+(edge.equals("N")?y-1:y)+","+(edge.equals("W")?"E":edge.equals("N")?"S":edge);
                if(!seen.add(key))throw new JSONException("Duplicate fence edge");
            }
        }
        return new PreviewWorld20(root.toString(),id,size,version,BlockTerrain.parse(size,source,null));
    }
}
