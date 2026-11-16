package dev.retrobit.gtlauncher;
import java.util.ArrayList;
import java.util.List;
/** Redirect at the current interpolated position without teleporting between tiles. */
final class GroundRedirect {
    static final class Result {
        final List<PortalTravel.Tile> route; final boolean reversed;
        Result(List<PortalTravel.Tile> route,boolean reversed){this.route=route;this.reversed=reversed;}
    }
    static Result plan(PortalTravel.Tile a,PortalTravel.Tile b,float progress,PortalTravel.Tile target,PortalTravel.Terrain terrain,int size) {
        List<PortalTravel.Tile> fromA=PortalTravel.path(a,target,terrain,size),fromB=PortalTravel.path(b,target,terrain,size);
        if(fromA==null && fromB==null)return null;
        float t=Math.max(0,Math.min(1,progress));
        boolean reverse=fromA!=null && (fromB==null || t+fromA.size()-1 < (1-t)+fromB.size()-1);
        List<PortalTravel.Tile> result=new ArrayList<>();result.add(reverse?b:a);result.addAll(reverse?fromA:fromB);
        return new Result(result,reverse);
    }
    private GroundRedirect() {}
}
