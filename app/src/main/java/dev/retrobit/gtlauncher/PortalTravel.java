package dev.retrobit.gtlauncher;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Pure portal routing rules. This class does not alter or import any worlds. */
final class PortalTravel {
    static final int SIZE = 9;
    static final class Tile {
        final int column, row;
        Tile(int column, int row) { this.column = column; this.row = row; }
        @Override public boolean equals(Object value) {
            return value instanceof Tile && ((Tile) value).column == column && ((Tile) value).row == row;
        }
        @Override public int hashCode() { return row * SIZE + column; }
    }
    static final class Portal {
        final String worldId, id, edge, destinationWorldId, destinationPortalId;
        final Tile tile;
        Portal(String worldId, String id, String edge, Tile tile, String destinationWorldId, String destinationPortalId) {
            this.worldId=worldId; this.id=id; this.edge=edge; this.tile=tile;
            this.destinationWorldId=destinationWorldId; this.destinationPortalId=destinationPortalId;
        }
    }
    interface Terrain {
        boolean canStand(int column, int row);
        boolean canStep(int fromColumn, int fromRow, int toColumn, int toRow);
    }
    private static boolean validTile(Tile tile) {
        return tile != null && tile.column >= 0 && tile.column < SIZE && tile.row >= 0 && tile.row < SIZE;
    }
    private static boolean isPortalOnEdge(Portal p) {
        if (p == null || p.worldId == null || p.id == null || !validTile(p.tile) || p.edge == null) return false;
        switch (p.edge) {
            case "N": return p.tile.row == 0;
            case "S": return p.tile.row == SIZE - 1;
            case "W": return p.tile.column == 0;
            case "E": return p.tile.column == SIZE - 1;
            default: return false;
        }
    }
    private static String opposite(String edge) {
        switch (edge) { case "N": return "S"; case "S": return "N"; case "E": return "W"; case "W": return "E"; default: return ""; }
    }
    static boolean arePaired(Portal a, Portal b) {
        return isPortalOnEdge(a) && isPortalOnEdge(b) && !a.worldId.equals(b.worldId)
                && opposite(a.edge).equals(b.edge)
                && b.worldId.equals(a.destinationWorldId) && b.id.equals(a.destinationPortalId)
                && a.worldId.equals(b.destinationWorldId) && a.id.equals(b.destinationPortalId);
    }
    /** Return a path including both endpoints, or null. Never choose a nearby substitute tile. */
    static List<Tile> pathToPortal(Tile start, Portal source, Terrain terrain) {
        if (!validTile(start) || !isPortalOnEdge(source) || terrain == null
                || !terrain.canStand(start.column, start.row) || !terrain.canStand(source.tile.column, source.tile.row)) return null;
        Tile[][] previous = new Tile[SIZE][SIZE]; boolean[][] visited = new boolean[SIZE][SIZE];
        ArrayDeque<Tile> queue = new ArrayDeque<>(); queue.add(start); visited[start.row][start.column] = true;
        final int[][] steps = {{0,-1},{1,0},{0,1},{-1,0}};
        while (!queue.isEmpty()) {
            Tile current = queue.remove();
            if (current.equals(source.tile)) {
                ArrayList<Tile> route = new ArrayList<>();
                for (Tile p=current; p!=null; p=previous[p.row][p.column]) route.add(p);
                Collections.reverse(route); return route;
            }
            for (int[] step : steps) {
                int c=current.column+step[0], r=current.row+step[1];
                if (c<0 || c>=SIZE || r<0 || r>=SIZE || visited[r][c] || !terrain.canStand(c,r)
                        || !terrain.canStep(current.column,current.row,c,r)) continue;
                Tile next=new Tile(c,r); previous[r][c]=current; visited[r][c]=true; queue.add(next);
            }
        }
        return null;
    }
    /** Confirmation is required by the caller; this checks conditions again at confirmation time. */
    static boolean canTravel(Portal source, Portal target, Tile character, Terrain destination) {
        return arePaired(source,target) && source.tile.equals(character) && destination != null
                && destination.canStand(target.tile.column,target.tile.row);
    }
    private PortalTravel() {}
}
