package clojure_vulkan.math;

import clojure.lang.Keyword;
import org.lwjgl.vulkan.VK13;

import java.util.HashMap;
import java.util.Map;

public class GLSL {
    private GLSL(){
    }
    public interface GLSLType {
        int componentsCount();

        int componentSizeof();

        int sizeof();

        int format();
    }

    private static class GLSLFloat implements GLSLType {
        public int componentsCount() {
            return 1;
        }

        public int componentSizeof() {
            return Float.BYTES;
        }

        public int sizeof() {
            return Float.BYTES;
        }

        public int format() {
            return VK13.VK_FORMAT_R32_SFLOAT;
        }
    }

    private static class GLSLVec2f implements GLSLType {
        public int componentsCount() {
            return 2;
        }

        public int componentSizeof() {
            return Float.BYTES;
        }

        public int sizeof() {
            return 2 * Float.BYTES;
        }

        public int format() {
            return VK13.VK_FORMAT_R32G32_SFLOAT;
        }
    }

    private static class GLSLVec3f implements GLSLType {
        public int componentsCount() {
            return 3;
        }

        public int componentSizeof() {
            return Float.BYTES;
        }

        public int sizeof() {
            return 3 * Float.BYTES;
        }

        public int format() {
            return VK13.VK_FORMAT_R32G32B32_SFLOAT;
        }
    }

    private static class GLSLVec4f implements GLSLType {
        public int componentsCount() {
            return 4;
        }

        public int componentSizeof() {
            return Float.BYTES;
        }

        public int sizeof() {
            return 4 * Float.BYTES;
        }

        public int format() {
            return VK13.VK_FORMAT_R32G32B32A32_SFLOAT;
        }
    }

    private static final Keyword
            FLOAT_KW = Keyword.intern("float"),
            VEC2_KW = Keyword.intern("vec2"),
            VEC3_KW = Keyword.intern("vec3"),
            VEC4_KW = Keyword.intern("vec4");

    public static final HashMap<Keyword, GLSLType> keywordToType = new HashMap<>(Map.of(
            FLOAT_KW, new GLSLFloat(),
            VEC2_KW, new GLSLVec2f(),
            VEC3_KW, new GLSLVec3f(),
            VEC4_KW, new GLSLVec4f()
    ));
}
