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
        if (version > 3) throw new JSONException("Unsupported world version");
        JSONObject w = root.getJSONObject("world");
        keys(w,"id","name","description","color","showCharacter","characterColumn","characterRow","characterScale","characterFacing","house","gridSize","tiles");
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
        return new PreviewWorld20(root.toString(),id,size,version,BlockTerrain.parse(size,source,null));
    }
}
