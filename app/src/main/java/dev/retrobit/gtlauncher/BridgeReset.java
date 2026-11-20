package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Lossless one-time reset of placed bridges in imported preview worlds. */
final class BridgeReset {
    static String clean(String raw) throws JSONException {
        JSONArray worlds=new JSONArray(raw);boolean changed=false;
        for(int i=0;i<worlds.length();i++) {
            JSONObject entry=worlds.getJSONObject(i);
            if(!entry.has("previewExport"))continue;
            String original=entry.getString("previewExport");
            PreviewWorld20.parse(original); // Refuse to rewrite anything unsupported.
            JSONObject root=new JSONObject(original),world=root.getJSONObject("world");
            JSONArray bridges=world.optJSONArray("bridges");if(bridges==null || bridges.length()==0)continue;
            world.remove("bridges");changed=true;
            int size=world.getInt("gridSize"),col=world.getInt("characterColumn"),row=world.getInt("characterRow");
            JSONArray tiles=world.getJSONArray("tiles");
            if(tiles.getInt(row*size+col)==0) {
                int bestX=-1,bestY=-1,bestWater=2,bestDistance=Integer.MAX_VALUE;
                JSONArray trees=world.optJSONArray("orangeTrees");JSONObject house=world.optJSONObject("house");
                for(int y=0;y<size;y++)for(int x=0;x<size;x++) {
                    int tile=tiles.getInt(y*size+x);if(tile==0)continue;
                    if(house!=null && x>=house.getInt("column") && x<house.getInt("column")+3 && y>=house.getInt("row") && y<house.getInt("row")+3)continue;
                    boolean blocked=false;
                    if(trees!=null)for(int t=0;t<trees.length();t++) {JSONObject tree=trees.getJSONObject(t);String kind=tree.optString("kind");if(kind.equals("wild-grass")||kind.equals("bush"))continue;
                        int tx=tree.getInt("column"),ty=tree.getInt("row"),n=tree.optInt("size",2);if(x>=tx && x<tx+n && y>=ty && y<ty+n){blocked=true;break;}}
                    if(blocked)continue;int water=tile==3?1:0,distance=Math.abs(x-col)+Math.abs(y-row);
                    if(water<bestWater || (water==bestWater && distance<bestDistance)){bestX=x;bestY=y;bestWater=water;bestDistance=distance;}
                }
                if(bestX<0)throw new JSONException("No safe character tile after bridge removal");
                world.put("characterColumn",bestX);world.put("characterRow",bestY);
            }
            PreviewWorld20.parse(root.toString());
            entry.put("previewExport",root.toString());
        }
        return changed?worlds.toString():raw;
    }
    private BridgeReset() {}
}
