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

    public static void memcpyIntegers(ByteBuffer buf, int[] data) {
        for (int i : data)
            buf.putInt(i);
    }

    public static void memcpyBytes(ByteBuffer buf, byte[] data) {
        for (byte b : data)
            buf.put(b);
    }

    public static void memcpyLongs(ByteBuffer buf, long[] data) {
        for (long l : data)
            buf.putLong(l);
    }

    public static void memcpyUBO(ByteBuffer buf, UniformBufferObject ubo) {
        ubo.copyInfoByteBuffer(buf);
    }
}
