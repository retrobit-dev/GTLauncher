package dev.retrobit.gtlauncher;

/** Screen-space island scale and its inverse, for consistent drawing and hit tests. */
final class IslandProjection {
    static final float BASE_ZOOM = 4f / 7f;
    static float scale(float viewSize,float sceneSize) { return BASE_ZOOM * sceneSize * 2f / viewSize; }
    static float toScene(float screenValue,float viewSize,float sceneSize,float anchor) {
        return anchor+(screenValue*sceneSize/viewSize-anchor)/scale(viewSize,sceneSize);
    }
    private IslandProjection() {}
}
