package clojure_vulkan;

import org.joml.Matrix4f;

import java.nio.ByteBuffer;

public class UniformBufferObject {
    private Matrix4f model;
    private Matrix4f view;
    private Matrix4f projection;

    public UniformBufferObject(Matrix4f model, Matrix4f view, Matrix4f projection) {
        this.model = model;
        this.view = view;
        this.projection = projection;
    }

    public void copyInfoByteBuffer(ByteBuffer buf) {
        model.get(buf);
        view.get(buf);
        projection.get(buf);
    }
}
