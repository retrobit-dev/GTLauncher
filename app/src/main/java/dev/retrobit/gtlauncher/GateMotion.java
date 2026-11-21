package dev.retrobit.gtlauncher;

/** Stateless gate opening based on distance from the player's tile-space position. */
final class GateMotion {
    private GateMotion() {}
    static double openness(int column,int row,String edge,double playerColumn,double playerRow) {
        if(!Double.isFinite(playerColumn) || !Double.isFinite(playerRow) || edge==null)return 0;
        double x1,y1,x2,y2;
        switch(edge) {
            case "N": x1=column-.5;y1=row-.5;x2=column+.5;y2=y1;break;
            case "S": x1=column-.5;y1=row+.5;x2=column+.5;y2=y1;break;
            case "W": x1=column-.5;y1=row-.5;x2=x1;y2=row+.5;break;
            case "E": x1=column+.5;y1=row-.5;x2=x1;y2=row+.5;break;
            default:return 0;
        }
        double dx=x2-x1,dy=y2-y1,t=Math.max(0,Math.min(1,((playerColumn-x1)*dx+(playerRow-y1)*dy)/(dx*dx+dy*dy)));
        double distance=Math.hypot(playerColumn-(x1+t*dx),playerRow-(y1+t*dy));
        double p=Math.max(0,Math.min(1,(1-distance)*2));return p*p*(3-2*p);
    }
}
