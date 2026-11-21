package dev.retrobit.gtlauncher;

/** Stateless gate opening based on distance from the player's tile-space position. */
final class GateMotion {
    private GateMotion() {}
    static final class State {
        final double amount; final int side;
        State(double amount,int side){this.amount=amount;this.side=side;}
    }
    static State update(int column,int row,String edge,double playerColumn,double playerRow,int previousSide) {
        double amount=openness(column,row,edge,playerColumn,playerRow);
        if(amount==0)return new State(0,0);
        boolean vertical="E".equals(edge)||"W".equals(edge);
        double boundary=vertical?column+("E".equals(edge)?.5:-.5):row+("S".equals(edge)?.5:-.5);
        double position=vertical?playerColumn:playerRow;
        int side=previousSide==1 || previousSide==-1?previousSide:position<boundary?-1:1;
        return new State(amount,side);
    }
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
