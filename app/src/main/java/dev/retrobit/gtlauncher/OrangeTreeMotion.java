package dev.retrobit.gtlauncher;

/** Visible wind-driven sway for the combined orange-tree canopy and fruit sprite. */
final class OrangeTreeMotion {
    static double sway(boolean enabled,double speed,double direction,double heading,double time,int column,int row) {
        if(!enabled || !Double.isFinite(speed) || speed<=0 || !Double.isFinite(time)) return 0;
        double bearing=Math.toRadians((Double.isFinite(direction)?direction:90)+180-(Double.isFinite(heading)?heading:0));
        double projection=Math.sin(bearing)+.25*Math.cos(bearing);
        double sign=projection<0?-1:1, amplitude=Math.min(2.8,speed*.26);
        return sign*amplitude*(.16+.84*Math.sin(time*.72+column*1.73+row*2.31));
    }
    private OrangeTreeMotion() {}
}
