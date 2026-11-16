package dev.retrobit.gtlauncher;

/** Frame-rate independent follow with a dead zone and a bounded catch-up rate. */
final class CameraFollow {
    private boolean ready;
    private float x,y;
    void reset() { ready=false; }
    boolean ready() { return ready; }
    float x() { return x; }
    float y() { return y; }
    boolean advance(float targetX,float targetY,float marginX,float marginY,float deltaSeconds,boolean snap) {
        if(!ready || Math.hypot(targetX-x,targetY-y)>168f) {
            x=targetX;y=targetY;ready=true;return false;
        }
        float desiredX=Math.max(targetX-marginX,Math.min(targetX+marginX,x));
        float desiredY=Math.max(targetY-marginY,Math.min(targetY+marginY,y));
        float dt=Math.max(0,Math.min(.1f,deltaSeconds));
        float factor=snap?1f:(float)(1-Math.exp(-dt/.16));
        x+=(desiredX-x)*factor;y+=(desiredY-y)*factor;
        return Math.hypot(desiredX-x,desiredY-y)>.01;
    }
}
