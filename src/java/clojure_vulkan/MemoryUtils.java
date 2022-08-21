package clojure_vulkan;

import java.nio.ByteBuffer;

public class MemoryUtils {
    public static void memcpyFloats(ByteBuffer buf, float[] data) {
        for (float f : data)
            buf.putFloat(f);
    }

    public static void memcpyShorts(ByteBuffer buf, short[] data) {
        for (short s : data)
            buf.putShort(s);
    }
}
