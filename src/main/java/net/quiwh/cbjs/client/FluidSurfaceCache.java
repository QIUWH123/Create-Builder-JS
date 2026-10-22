package net.quiwh.cbjs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** Bounded, CPU-only geometry cache. Sprite identity invalidates atlas UVs on resource reload. */
final class FluidSurfaceCache {
    private static final int MAX_VERTICES = 65536;
    private static final LinkedHashMap<Key, Mesh> MESHES = new LinkedHashMap<>(16, .75f, true);
    private static int vertices;

    record Key(TextureAtlasSprite sprite, int color, int light, int faces, boolean gas,
               float xMin, float yMin, float zMin, float xMax, float yMax, float zMax) {
        boolean smallEnough() {
            double x = Math.ceil(xMax - xMin) + 1, y = Math.ceil(yMax - yMin) + 1,
                    z = Math.ceil(zMax - zMin) + 1;
            return (x * y + y * z + x * z) * 8 <= 16384;
        }
    }

    static void render(Key key, boolean stable, PoseStack pose, VertexConsumer target,
                       BiConsumer<PoseStack, VertexConsumer> nativeGeometry) {
        // Very large/continuously changing surfaces retain the native streaming path.
        if (!stable || !key.smallEnough()) { nativeGeometry.accept(pose, target); return; }
        Mesh mesh = MESHES.get(key);
        if (mesh == null) {
            mesh = new Mesh();
            nativeGeometry.accept(new PoseStack(), mesh);
            mesh.trim();
            while (!MESHES.isEmpty() && (MESHES.size() >= 64 || vertices + mesh.count > MAX_VERTICES)) {
                var oldest = MESHES.entrySet().iterator();
                vertices -= oldest.next().getValue().count;
                oldest.remove();
            }
            MESHES.put(key, mesh);
            vertices += mesh.count;
        }
        mesh.draw(pose, target);
    }

    private static final class Mesh implements VertexConsumer {
        private float[] geometry = new float[1024];
        private int[] attributes = new int[384];
        private int count, color = -1, overlay, light;
        private float x, y, z, u, v, nx, ny, nz;

        void trim() { geometry = Arrays.copyOf(geometry, count * 8); attributes = Arrays.copyOf(attributes, count * 3); }
        void draw(PoseStack stack, VertexConsumer target) {
            var pose = stack.last();
            for (int i = 0; i < count; i++) {
                int f = i * 8, a = i * 3;
                target.vertex(pose.pose(), geometry[f], geometry[f + 1], geometry[f + 2])
                        .color(attributes[a]).uv(geometry[f + 3], geometry[f + 4])
                        .overlayCoords(attributes[a + 1]).uv2(attributes[a + 2])
                        .normal(pose.normal(), geometry[f + 5], geometry[f + 6], geometry[f + 7]).endVertex();
            }
        }
        @Override public VertexConsumer vertex(double x, double y, double z) { this.x=(float)x; this.y=(float)y; this.z=(float)z; return this; }
        @Override public VertexConsumer color(int r, int g, int b, int a) { color=(a<<24)|(r<<16)|(g<<8)|b; return this; }
        @Override public VertexConsumer uv(float u, float v) { this.u=u; this.v=v; return this; }
        @Override public VertexConsumer overlayCoords(int u, int v) { overlay=(v<<16)|u; return this; }
        @Override public VertexConsumer uv2(int u, int v) { light=(v<<16)|u; return this; }
        @Override public VertexConsumer normal(float x, float y, float z) { nx=x; ny=y; nz=z; return this; }
        @Override public void endVertex() {
            int f=count*8, a=count*3;
            if (f+8>geometry.length) geometry=Arrays.copyOf(geometry, geometry.length*2);
            if (a+3>attributes.length) attributes=Arrays.copyOf(attributes, attributes.length*2);
            geometry[f]=x; geometry[f+1]=y; geometry[f+2]=z; geometry[f+3]=u; geometry[f+4]=v;
            geometry[f+5]=nx; geometry[f+6]=ny; geometry[f+7]=nz;
            attributes[a]=color; attributes[a+1]=overlay; attributes[a+2]=light; count++;
        }
        @Override public void defaultColor(int r,int g,int b,int a) { color(r,g,b,a); }
        @Override public void unsetDefaultColor() { color=-1; }
    }
    private FluidSurfaceCache() { }
}
