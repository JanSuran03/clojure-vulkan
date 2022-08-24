package clojure_vulkan;

import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class UniformBufferObject {
    private final Matrix4f model;
    private final Matrix4f view;
    private final Matrix4f projection;
    private static final int sizeOfMatrix4f = Float.BYTES * 16;

    public UniformBufferObject(Matrix4f model, Matrix4f view, Matrix4f projection) {
        this.model = model;
        this.view = view;
        this.projection = projection;
    }

    public void copyInfoByteBuffer(ByteBuffer buf) {
        model.get(buf);
        view.get(sizeOfMatrix4f, buf);
        projection.get(2 * sizeOfMatrix4f, buf);
    }
}
