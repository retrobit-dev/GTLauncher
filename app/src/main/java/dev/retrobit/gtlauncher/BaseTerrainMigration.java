package dev.retrobit.gtlauncher;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Upgrade only the recognizable legacy Base prefix, keeping every other block. */
final class BaseTerrainMigration {
    static String upgrade(String raw) throws JSONException {
        JSONArray worlds=new JSONArray(raw);boolean changed=false;
        for(int i=0;i<worlds.length();i++) {
            JSONObject entry=worlds.getJSONObject(i);if(!entry.has("previewExport"))continue;
            String original=entry.getString("previewExport");PreviewWorld20.parse(original);
            JSONObject root=new JSONObject(original),world=root.getJSONObject("world");
            JSONArray stacks=world.optJSONArray("blockStacks");if(stacks==null)continue;
            JSONArray tiles=world.getJSONArray("tiles");boolean worldChanged=false;
            for(int n=0;n<stacks.length();n++) {
                JSONArray column=stacks.getJSONArray(n);if(column.length()<5)continue;
                boolean template=column.getInt(4)==1;
                for(int level=0;level<4;level++)if(column.getInt(level)!=2)template=false;
                if(!template)continue;
                column.put(4,7);if(column.length()==5)tiles.put(n,7);worldChanged=true;
            }
            if(worldChanged){root.put("version",Math.max(14,root.getInt("version")));String result=root.toString();PreviewWorld20.parse(result);entry.put("previewExport",result);changed=true;}
        }
        return changed?worlds.toString():raw;
    }
    private BaseTerrainMigration(){}
}
