package dev.retrobit.gtlauncher;
/** Small, bounded projected shadow whose base remains at the fence endpoints. */
final class FenceShadow {
    private static float clamp(float v,float max){return Math.max(-max,Math.min(max,v));}
    static float[] offset(float ax,float ay,float bx,float by,float lightX,float lightY,float lift) {
        float dx=clamp(((ax+bx)/2-lightX)*.08f,Math.abs(bx-ax)*.30f);
        float dy=clamp(((ay+by)/2-lightY-lift)*.08f,Math.max(0,Math.min(12,25-Math.abs(by-ay))));
        if(Math.abs(dy)<.75f)dy=dy<0?-.75f:.75f;
        return new float[]{dx,dy};
    }
    private FenceShadow() {}
}
