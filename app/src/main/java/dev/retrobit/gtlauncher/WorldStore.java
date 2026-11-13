package dev.retrobit.gtlauncher;

import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import java.util.UUID;

/** Browser-exported world data stored locally on this Android device. */
final class WorldStore {
    private static final String WORLDS_KEY = "worlds_v1";
    private static final String ACTIVE_KEY = "active_world_v1";
    static final String BASE_ID = "base";
    private final SharedPreferences prefs;

    static final class SurfaceObject {
        final String type, edge, material, raw;
        final int column, row;
        SurfaceObject(String type,int column,int row,String edge,String material,String raw) {
            this.type=type;this.column=column;this.row=row;this.edge=edge;this.material=material;this.raw=raw;
        }
    }
    static final class World {
        final String id, name, description, color;
        final boolean showCharacter, showHouse, hasHouse;
        final int houseColumn, houseRow;
        final double houseScale;
        final String houseFacing, houseId;
        final int formatVersion;
        final int characterColumn, characterRow;
        final double characterScale;
        final String characterFacing;
        final List<PortalTravel.Portal> portals;
        final List<SurfaceObject> surfaces;
        World(String id, String name, String description, String color,
                boolean showCharacter, int column, int row, double scale, String facing,
                boolean hasHouse, boolean showHouse, int houseColumn, int houseRow, double houseScale, String houseFacing, String houseId, int formatVersion, List<PortalTravel.Portal> portals, List<SurfaceObject> surfaces) {
            this.id = id; this.name = name; this.description = description; this.color = color;
            this.showCharacter = showCharacter; this.characterColumn = column;
            this.characterRow = row; this.characterScale = scale; this.characterFacing = facing;
            this.hasHouse = hasHouse; this.showHouse = showHouse; this.houseColumn = houseColumn;
            this.houseRow = houseRow; this.houseScale = houseScale; this.houseFacing = houseFacing;
            this.houseId = houseId; this.formatVersion = formatVersion; this.portals = java.util.Collections.unmodifiableList(new ArrayList<>(portals)); this.surfaces = java.util.Collections.unmodifiableList(new ArrayList<>(surfaces));
        }
    }

    WorldStore(SharedPreferences prefs) { this.prefs = prefs; }

    private static String requiredString(JSONObject object, String key) throws JSONException {
        Object value = object.get(key);
        if (!(value instanceof String)) throw new JSONException("Invalid " + key);
        return (String) value;
    }

    private static World parseWorld(JSONObject object, String id, int formatVersion) throws JSONException {
        String name = requiredString(object, "name").trim();
        String description = requiredString(object, "description").trim();
        String color = requiredString(object, "color");
        if (name.isEmpty() || name.length() > 48 || description.length() > 160
                || !color.matches("#[0-9a-fA-F]{6}")) throw new JSONException("Invalid world fields");
        boolean visible = false, house = false, houseVisible = false;
        int houseColumn = 0, houseRow = 0;
        double houseScale = 1.0;
        String houseFacing = "S", houseId=null;
        int column = 4, row = 4;
        double scale = 1.0;
        String facing = "S";
        List<PortalTravel.Portal> portals = new ArrayList<>();
        List<SurfaceObject> surfaces = new ArrayList<>();
        java.util.Set<String> fenceIds=new java.util.HashSet<>(), fenceEdges=new java.util.HashSet<>(), roadTiles=new java.util.HashSet<>();
        if (object.has("objects")) {
            Object value = object.get("objects");
            if (!(value instanceof JSONArray)) throw new JSONException("Invalid objects");
            JSONArray objects = (JSONArray) value;
            if (objects.length() > (formatVersion >= 17 ? 606 : formatVersion >= 8 ? 283 : formatVersion >= 7 ? 202 : formatVersion >= 5 ? 2 : 1)) throw new JSONException("Unsupported objects");
            for (int i = 0; i < objects.length(); i++) {
                JSONObject character = objects.getJSONObject(i);
                String type = requiredString(character, "type");
                if (formatVersion >= 8 && "road".equals(type)) {
                    requireOnlyKeys(character,"type","column","row","material");
                    Object col=character.get("column"),rowObject=character.get("row");
                    if (!(col instanceof Number) || !(rowObject instanceof Number) || ((Number)col).doubleValue()!=Math.rint(((Number)col).doubleValue()) || ((Number)rowObject).doubleValue()!=Math.rint(((Number)rowObject).doubleValue())) throw new JSONException("Invalid road");
                    int c=((Number)col).intValue(),r=((Number)rowObject).intValue();String material=requiredString(character,"material");
                    if(c<0||c>8||r<0||r>8||!roadTiles.add(c+","+r)) throw new JSONException("Invalid road");
                    if(!material.equals("dirt") && !(formatVersion>=11 && material.equals("water"))) throw new JSONException("Unsupported object");
                    surfaces.add(new SurfaceObject(type,c,r,null,material,character.toString()));continue;
                }
                if (formatVersion >= 7 && "fence".equals(type)) {
                    requireOnlyKeys(character,"type","id","column","row","edge","variant","destinationWorldId","destinationPortalId");
                    String variant=character.has("variant") ? requiredString(character,"variant") : "";
                    if (!variant.isEmpty() && !variant.equals("portal")) throw new JSONException("Unsupported object");
                    if (variant.equals("portal") && formatVersion<22) throw new JSONException("Unsupported object");
                    String portalId=requiredString(character,"id"), edge=requiredString(character,"edge");
                    Object c=character.get("column"), r=character.get("row");
                    if (!portalId.matches("[A-Za-z0-9_-]{1,128}") || !(c instanceof Number) || !(r instanceof Number)
                            || ((Number)c).doubleValue()!=Math.rint(((Number)c).doubleValue()) || ((Number)r).doubleValue()!=Math.rint(((Number)r).doubleValue())) throw new JSONException("Invalid portal");
                    int col=((Number)c).intValue(), portalRow=((Number)r).intValue();
                    if (col<0 || col>8 || portalRow<0 || portalRow>8 || !(edge.equals("N")||edge.equals("E")||edge.equals("S")||edge.equals("W"))) throw new JSONException("Invalid fence edge");
                    int nc=col+(edge.equals("E")?1:edge.equals("W")?-1:0), nr=portalRow+(edge.equals("S")?1:edge.equals("N")?-1:0);
                    int start=portalRow*9+col, end=nr*9+nc;
                    String edgeKey=(nc>=0 && nc<9 && nr>=0 && nr<9) ? Math.min(start,end)+":"+Math.max(start,end) : col+","+portalRow+","+edge;
                    if(!fenceIds.add(portalId) || !fenceEdges.add(edgeKey)) throw new JSONException("Duplicate fence");
                    if (!variant.equals("portal")) {
                        if(character.has("destinationWorldId") || character.has("destinationPortalId")) throw new JSONException("Invalid fence pair");
                        surfaces.add(new SurfaceObject(type,col,portalRow,edge,null,character.toString()));continue;
                    }
                    if (!((edge.equals("N") && portalRow==0) || (edge.equals("S") && portalRow==8) || (edge.equals("W") && col==0) || (edge.equals("E") && col==8))) throw new JSONException("Invalid portal edge");
                    if (character.has("destinationWorldId") != character.has("destinationPortalId")) throw new JSONException("Incomplete portal pair");
                    String destWorld=null,destPortal=null;
                    if (character.has("destinationWorldId")) {
                        if(formatVersion<28) throw new JSONException("Unsupported object");
                        destWorld=requiredString(character,"destinationWorldId");destPortal=requiredString(character,"destinationPortalId");
                        if (!destWorld.matches("[A-Za-z0-9_-]{1,128}") || !destPortal.matches("[A-Za-z0-9_-]{1,128}")) throw new JSONException("Invalid portal pair");
                    }
                    for (PortalTravel.Portal previous : portals) if (previous.id.equals(portalId) || (previous.tile.column==col && previous.tile.row==portalRow && previous.edge.equals(edge))) throw new JSONException("Duplicate portal");
                    portals.add(new PortalTravel.Portal(id,portalId,edge,new PortalTravel.Tile(col,portalRow),destWorld,destPortal)); continue;
                }
                if ((formatVersion>=11 && "bush".equals(type)) || (formatVersion>=14 && "tree".equals(type)) || (formatVersion>=18 && "sunflower".equals(type)) || (formatVersion>=17 && "mailbox".equals(type)) || (formatVersion>=26 && "storage-box".equals(type))) {
                    if(type.equals("bush")) requireOnlyKeys(character,"type","column","row","variant");
                    else if(type.equals("tree")) requireOnlyKeys(character,"type","column","row","species");
                    else if(type.equals("sunflower") && formatVersion>=19) requireOnlyKeys(character,"type","column","row","facing");
                    else if(type.equals("storage-box") && formatVersion>=27) requireOnlyKeys(character,"type","column","row","facing");
                    else requireOnlyKeys(character,"type","column","row");
                    Object co=character.get("column"),ro=character.get("row");
                    if(!(co instanceof Number) || !(ro instanceof Number) || ((Number)co).doubleValue()!=Math.rint(((Number)co).doubleValue()) || ((Number)ro).doubleValue()!=Math.rint(((Number)ro).doubleValue())) throw new JSONException("Invalid object tile");
                    int c=((Number)co).intValue(),r=((Number)ro).intValue();
                    if(c<0||c>8||r<0||r>8) throw new JSONException("Invalid object tile");
                    String variant=type.equals("bush") ? requiredString(character,"variant") : type.equals("tree") ? requiredString(character,"species") : (type.equals("storage-box") || type.equals("sunflower")) && character.has("facing") ? requiredString(character,"facing") : type.equals("sunflower") ? "S" : "";
                    if(type.equals("bush") && !variant.equals("spiky") && !variant.equals("tiered") && !(formatVersion>=29 && variant.equals("wild-grass"))) throw new JSONException("Unsupported object");
                    if(type.equals("tree") && !variant.equals("mango") && !(formatVersion>=25 && (variant.equals("banana") || variant.equals("palm")))) throw new JSONException("Unsupported object");
                    if(type.equals("tree") && variant.equals("mango") && (c>7||r>7)) throw new JSONException("Invalid tree tile");
                    if(type.equals("sunflower") && !java.util.Arrays.asList("N","E","S","W").contains(variant)) throw new JSONException("Invalid direction");
                    if(type.equals("storage-box") && !variant.isEmpty() && !java.util.Arrays.asList("N","E","S","W").contains(variant)) throw new JSONException("Invalid direction");
                    for(SurfaceObject previous:surfaces) if(!previous.type.equals("road") && !previous.type.equals("fence") && previous.column==c && previous.row==r) throw new JSONException("Object collision");
                    surfaces.add(new SurfaceObject(type,c,r,null,variant,character.toString()));continue;
                }
                if ("house".equals(type)) {
                    if (formatVersion < 5 || house) throw new JSONException("Unsupported house");
                    if (formatVersion >= 6) {
                        if(formatVersion>=16) {
                            requireOnlyKeys(character,"type","id","column","row","scale","facing","visible");
                            if(character.has("id")) {houseId=requiredString(character,"id"); if(!houseId.matches("[A-Za-z0-9_-]{1,128}")) throw new JSONException("Invalid house id");}
                        } else requireOnlyKeys(character, "type", "column", "row", "scale", "facing", "visible");
                        Object hc = character.get("column"), hr = character.get("row"), hs = character.get("scale"), hv = character.get("visible");
                        if (!(hc instanceof Number) || !(hr instanceof Number) || !(hs instanceof Number) || !(hv instanceof Boolean))
                            throw new JSONException("Invalid house geometry");
                        double c = ((Number) hc).doubleValue(), r = ((Number) hr).doubleValue();
                        houseScale = ((Number) hs).doubleValue();
                        houseFacing = requiredString(character, "facing");
                        if (c != Math.rint(c) || r != Math.rint(r) || c < 0 || c > 6 || r < 0 || r > 6
                                || !Double.isFinite(houseScale) || houseScale < 0.5 || houseScale > 1.5
                                || !(houseFacing.equals("N") || houseFacing.equals("E") || houseFacing.equals("S") || houseFacing.equals("W")))
                            throw new JSONException("Invalid house geometry");
                        houseColumn = (int) c; houseRow = (int) r; houseVisible = (Boolean) hv;
                    } else {
                        requireOnlyKeys(character, "type", "column", "row");
                        Object hc = character.get("column"), hr = character.get("row");
                        if (!(hc instanceof Number) || !(hr instanceof Number)
                                || ((Number) hc).doubleValue() != 0 || ((Number) hr).doubleValue() != 0)
                            throw new JSONException("Unsupported house location");
                        houseVisible = true;
                    }
                    house = true; continue;
                }
                if (visible || !"character".equals(type)) throw new JSONException("Unsupported object");
                requireOnlyKeys(character, "type", "column", "row", "scale", "facing");
                if (formatVersion >= 4) {
                    facing = requiredString(character, "facing");
                    if (!facing.equals("N") && !facing.equals("E") && !facing.equals("S") && !facing.equals("W"))
                        throw new JSONException("Invalid character direction");
                } else if (character.has("facing")) throw new JSONException("Unexpected direction");
                if (!"character".equals(requiredString(character, "type"))) throw new JSONException("Unsupported object");
                Object col = character.get("column"), r = character.get("row"), size = character.get("scale");
                if (!(col instanceof Number) || !(r instanceof Number) || !(size instanceof Number))
                    throw new JSONException("Invalid character geometry");
                double colValue = ((Number) col).doubleValue(), rowValue = ((Number) r).doubleValue();
                scale = ((Number) size).doubleValue();
                if (colValue != Math.rint(colValue) || rowValue != Math.rint(rowValue)
                        || colValue < 0 || colValue > 8 || rowValue < 0 || rowValue > 8
                        || !Double.isFinite(scale) || scale < 0.5 || scale > 1.5)
                    throw new JSONException("Invalid character geometry");
                column = (int) colValue; row = (int) rowValue; visible = true;
            }
        }
        // New one-tile plants must never hide under an existing blocker, even if the
        // objects arrive in the opposite order in the file.
        for (SurfaceObject plant : surfaces) {
            boolean tropical=plant.type.equals("tree") && (plant.material.equals("banana") || plant.material.equals("palm"));
            if (!tropical && !plant.type.equals("sunflower")) continue;
            int c=plant.column,r=plant.row;
            if (visible && c==column && r==row || house && houseVisible && c>=houseColumn && c<=houseColumn+2 && r>=houseRow && r<=houseRow+2) throw new JSONException("Object collision");
            for (SurfaceObject other:surfaces) {
                if (other==plant || other.type.equals("fence")) continue;
                if (other.type.equals("road") && !"water".equals(other.material)) continue;
                int size=other.type.equals("tree") && other.material.equals("mango")?2:1;
                if(c>=other.column && c<other.column+size && r>=other.row && r<other.row+size) throw new JSONException("Object collision");
            }
        }
        return new World(id, name, description, color, visible, column, row, scale, facing,
                house, houseVisible, houseColumn, houseRow, houseScale, houseFacing, houseId, formatVersion, portals, surfaces);
    }

    private static void requireOnlyKeys(JSONObject object, String... allowed) throws JSONException {
        Iterator<String> keys = object.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            boolean found = false;
            for (String item : allowed) if (item.equals(key)) found = true;
            if (!found) throw new JSONException("Unsupported field: " + key);
        }
    }

    static World parseExport(String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!"gtlauncher-world".equals(requiredString(root, "format"))
                || !(root.get("version") instanceof Number)
                || !(root.get("world") instanceof JSONObject)) {
            throw new JSONException("Unsupported world format");
        }
        double version = ((Number) root.get("version")).doubleValue();
        JSONObject object = root.getJSONObject("world");
        if (version == 1.0) {
            // Legacy exports had no stable identifier; reject unknown fields rather than discard them.
            requireOnlyKeys(root, "format", "version", "world");
            requireOnlyKeys(object, "name", "description", "color");
            return parseWorld(object, UUID.randomUUID().toString(), 1);
        }
        if (version != 2.0 && version != 3.0 && version != 4.0 && version != 5.0 && version != 6.0 && version != 7.0 && version != 8.0 && version != 11.0 && version != 18.0 && version != 19.0 && version != 22.0 && version != 25.0 && version != 27.0 && version != 28.0 && version != 29.0) throw new JSONException("Unsupported world version");
        requireOnlyKeys(root, "format", "version", "world");
        requireOnlyKeys(object, "id", "name", "description", "color", "objects");
        String id = requiredString(object, "id");
        if (!id.matches("[A-Za-z0-9_-]{1,128}") || BASE_ID.equals(id))
            throw new JSONException("Invalid world id");
        Object objects = object.get("objects");
        if (!(objects instanceof JSONArray) || (version == 2.0 && ((JSONArray) objects).length() != 0))
            throw new JSONException("Unsupported world objects");
        return parseWorld(object, id, (int) version);
    }

    private static int storedVersion(JSONObject item) throws JSONException {
        if (!item.has("objects")) return 3;
        Object value = item.get("objects");
        if (!(value instanceof JSONArray)) throw new JSONException("Invalid objects");
        JSONArray objects = (JSONArray) value;
        int version = 3;
        for (int i = 0; i < objects.length(); i++) {
            JSONObject obj = objects.getJSONObject(i);
            if ("sunflower".equals(obj.optString("type")) || "tree".equals(obj.optString("type")) && ("banana".equals(obj.optString("species")) || "palm".equals(obj.optString("species"))) || "bush".equals(obj.optString("type")) && "wild-grass".equals(obj.optString("variant"))) version = Math.max(version, 29);
            else if ("fence".equals(obj.optString("type")) || "road".equals(obj.optString("type")) || "bush".equals(obj.optString("type")) || "tree".equals(obj.optString("type")) || "mailbox".equals(obj.optString("type")) || "storage-box".equals(obj.optString("type"))) version = Math.max(version, 28);
            else if ("house".equals(obj.optString("type"))) version = Math.max(version, obj.has("id") ? 28 : obj.has("visible") || obj.has("scale") || obj.has("facing") ? 6 : 5);
            else if (obj.has("facing")) version = Math.max(version, 4);
        }
        return version;
    }

    List<World> list() throws JSONException {
        JSONArray array = new JSONArray(prefs.getString(WORLDS_KEY, "[]"));
        if (array.length() > 1000) throw new JSONException("Too many worlds");
        List<World> worlds = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            String id = requiredString(item, "id");
            if (id.isEmpty() || BASE_ID.equals(id)) throw new JSONException("Invalid world id");
            for (World previous : worlds) if (previous.id.equals(id)) throw new JSONException("Duplicate world id");
            worlds.add(parseWorld(item, id, storedVersion(item)));
        }
        return worlds;
    }

    String activeId() { return prefs.getString(ACTIVE_KEY, BASE_ID); }

    World activeWorld() throws JSONException {
        String id = activeId();
        for (World world : list()) if (world.id.equals(id)) return world;
        return null; // Base World (or the removed built-in Home) has the base appearance
    }

    boolean select(String id) throws JSONException {
        boolean found = BASE_ID.equals(id);
        for (World world : list()) if (world.id.equals(id)) found = true;
        return found && prefs.edit().putString(ACTIVE_KEY, id).commit();
    }

    boolean upsertAndSelect(World world) throws JSONException {
        List<World> existing = list(); // Never overwrite unreadable stored data.
        boolean found = false;
        for (int i = 0; i < existing.size(); i++) {
            if (existing.get(i).id.equals(world.id)) {
                if (world.formatVersion < 25 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("tree") && (o.material.equals("banana") || o.material.equals("palm")))) throw new JSONException("Legacy export cannot replace tropical trees");
                if (world.formatVersion < 18 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("sunflower")) || world.formatVersion < 19 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("sunflower") && !o.material.equals("S"))) throw new JSONException("Legacy export cannot replace sunflower");
                if (world.formatVersion < 29 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("bush") && o.material.equals("wild-grass"))) throw new JSONException("Legacy export cannot replace wild grass");
                if (world.formatVersion < 16 && existing.get(i).houseId != null) throw new JSONException("Legacy export cannot replace house identity");
                if (world.formatVersion < 27 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("storage-box") && !o.material.isEmpty())) throw new JSONException("Legacy export cannot replace storage direction");
                if (world.formatVersion < 26 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("storage-box"))) throw new JSONException("Legacy export cannot replace storage");
                if (world.formatVersion < 17 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("mailbox"))) throw new JSONException("Legacy export cannot replace mailbox");
                if (world.formatVersion < 14 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("tree"))) throw new JSONException("Legacy export cannot replace tree");
                if (world.formatVersion < 11 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("bush"))) throw new JSONException("Legacy export cannot replace bush");
                if (world.formatVersion < 8 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("road"))) throw new JSONException("Legacy export cannot replace road data");
                if (world.formatVersion < 7 && existing.get(i).surfaces.stream().anyMatch(o -> o.type.equals("fence"))) throw new JSONException("Legacy export cannot replace fence data");
                if (world.formatVersion < 22 && !existing.get(i).portals.isEmpty()) throw new JSONException("Legacy export cannot replace portal data");
                if (world.formatVersion < 11 && existing.get(i).surfaces.stream().anyMatch(o -> "water".equals(o.material))) throw new JSONException("Legacy export cannot replace water data");
                if (world.formatVersion < 28 && existing.get(i).portals.stream().anyMatch(p -> p.destinationWorldId != null))
                    throw new JSONException("Legacy export cannot replace portal data");
                // A legacy file cannot express characters: reject rather than silently erase one.
                if (world.formatVersion < 3 && existing.get(i).showCharacter)
                    throw new JSONException("Legacy export cannot replace a world with a character");
                if (world.formatVersion < 4 && existing.get(i).showCharacter
                        && !"S".equals(existing.get(i).characterFacing))
                    throw new JSONException("Legacy export cannot replace a rotated character");
                if (world.formatVersion < 5 && existing.get(i).hasHouse)
                    throw new JSONException("Legacy export cannot replace a world with a house");
                if (world.formatVersion == 5 && existing.get(i).hasHouse &&
                        (existing.get(i).houseColumn != 0 || existing.get(i).houseRow != 0 || existing.get(i).houseScale != 1.0
                        || !"S".equals(existing.get(i).houseFacing) || !existing.get(i).showHouse))
                    throw new JSONException("Legacy export cannot replace edited house");
                existing.set(i, world);
                found = true;
                break;
            }
        }
        if (!found) {
            if (existing.size() >= 1000) throw new JSONException("Too many worlds");
            existing.add(world);
        }
        return saveWorlds(existing, world.id);
    }

    private boolean saveWorlds(List<World> worlds, String activeId) throws JSONException {
        JSONArray array = new JSONArray();
        for (World item : worlds) {
            JSONObject object = new JSONObject();
            object.put("id", item.id); object.put("name", item.name);
            object.put("description", item.description); object.put("color", item.color);
            JSONArray objects = new JSONArray();
            if (item.showCharacter) {
                JSONObject character = new JSONObject();
                character.put("type", "character"); character.put("column", item.characterColumn);
                character.put("row", item.characterRow); character.put("scale", item.characterScale);
                character.put("facing", item.characterFacing);
                objects.put(character);
            }
            if (item.hasHouse) {
                JSONObject house = new JSONObject();
                house.put("type", "house"); if(item.houseId!=null) house.put("id",item.houseId); house.put("column", item.houseColumn); house.put("row", item.houseRow);
                house.put("scale", item.houseScale); house.put("facing", item.houseFacing); house.put("visible", item.showHouse);
                objects.put(house);
            }
            for (SurfaceObject surface : item.surfaces) objects.put(new JSONObject(surface.raw));
            for (PortalTravel.Portal portal : item.portals) {
                JSONObject p=new JSONObject();p.put("type","fence");p.put("variant","portal");p.put("id",portal.id);
                p.put("column",portal.tile.column);p.put("row",portal.tile.row);p.put("edge",portal.edge);
                if(portal.destinationWorldId!=null){p.put("destinationWorldId",portal.destinationWorldId);p.put("destinationPortalId",portal.destinationPortalId);}
                objects.put(p);
            }
            object.put("objects", objects);
            array.put(object);
        }
        return prefs.edit().putString(WORLDS_KEY, array.toString())
                .putString(ACTIVE_KEY, activeId).commit();
    }

    boolean moveCharacter(String worldId, int column, int row, String facing) throws JSONException {
        if (!worldId.equals(activeId()) || column<0 || column>8 || row<0 || row>8 || !java.util.Arrays.asList("N","E","S","W").contains(facing)) return false;
        List<World> worlds=list();
        for(int i=0;i<worlds.size();i++) { World w=worlds.get(i); if(w.id.equals(worldId) && w.showCharacter) {
            worlds.set(i, new World(w.id,w.name,w.description,w.color,true,column,row,w.characterScale,facing,w.hasHouse,w.showHouse,w.houseColumn,w.houseRow,w.houseScale,w.houseFacing,w.houseId,w.formatVersion,w.portals,w.surfaces));
            return saveWorlds(worlds,worldId);
        }}
        return false;
    }
    boolean travel(String sourceWorldId, String portalId) throws JSONException {
        if (!sourceWorldId.equals(activeId())) return false;
        List<World> worlds=list(); World source=null,destination=null; PortalTravel.Portal from=null,to=null;
        for(World w:worlds) if(w.id.equals(sourceWorldId)) source=w;
        if(source==null || !source.showCharacter) return false;
        for(PortalTravel.Portal p:source.portals) if(p.id.equals(portalId)) from=p;
        if(from==null) return false;
        for(World w:worlds) if(w.id.equals(from.destinationWorldId)) destination=w;
        if(destination==null) return false;
        for(PortalTravel.Portal p:destination.portals) if(p.id.equals(from.destinationPortalId)) to=p;
        final World dest=destination;
        PortalTravel.Terrain terrain=new PortalTravel.Terrain() {
            public boolean canStand(int c,int r){if(dest.showHouse && c>=dest.houseColumn && c<=dest.houseColumn+2 && r>=dest.houseRow && r<=dest.houseRow+2) return false;for(SurfaceObject o:dest.surfaces) {if(o.type.equals("tree") && c>=o.column && c<o.column+(o.material.equals("mango")?2:1) && r>=o.row && r<o.row+(o.material.equals("mango")?2:1)) return false;if((o.type.equals("bush")||o.type.equals("sunflower")||o.type.equals("mailbox")||o.type.equals("storage-box")||(o.type.equals("road") && "water".equals(o.material))) && o.column==c && o.row==r)return false;}return true;}
            public boolean canStep(int a,int b,int c,int d){return true;}
        };
        if(!PortalTravel.canTravel(from,to,new PortalTravel.Tile(source.characterColumn,source.characterRow),terrain)) return false;
        for(int i=0;i<worlds.size();i++) if(worlds.get(i).id.equals(destination.id)) {
            worlds.set(i,new World(dest.id,dest.name,dest.description,dest.color,true,to.tile.column,to.tile.row,source.characterScale,source.characterFacing,dest.hasHouse,dest.showHouse,dest.houseColumn,dest.houseRow,dest.houseScale,dest.houseFacing,dest.houseId,dest.formatVersion,dest.portals,dest.surfaces)); break;
        }
        return saveWorlds(worlds,dest.id);
    }
}
