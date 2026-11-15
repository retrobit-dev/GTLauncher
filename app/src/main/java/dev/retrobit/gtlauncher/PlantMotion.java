package dev.retrobit.gtlauncher;

/** Cosmetic wind motion in scene units; independent of persistent world data. */
final class PlantMotion {
    static double sway(boolean enabled,double speed,double windDirection,double heading,
                       double time,int column,int row,String type,String material) {
        if(!enabled || speed<1)return 0;
        double bearing=Math.toRadians(windDirection+180-heading);
        double projected=Math.sin(bearing)+.25*Math.cos(bearing);
        double direction=Math.abs(projected)<.35?(projected<0?-.35:.35):projected;
        double strength=Math.min(1,.25+speed/35);
        boolean tropical=type.equals("tree") && (material.equals("banana")||material.equals("palm"));
        double phase=column*1.73+row*2.31,frequency=tropical?.6:.9;
        double amplitude=type.equals("tree")?(tropical?3.0:4.0):type.equals("sunflower")?1.8:2.2;
        return amplitude*strength*direction*(.18+.82*Math.sin(time*frequency+phase));
    }
    private PlantMotion() {}
}
