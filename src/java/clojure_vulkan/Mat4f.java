package clojure_vulkan;

import org.joml.Matrix4f;

public class Mat4f {
    public static Matrix4f vulkanize(Matrix4f m) {
        return m.m11(-m.m11());
    }
}
