package clojure_vulkan.Vulkan;

import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VK13;

public class Buffer implements NativeResource {
    public Buffer() {
    }

    public long bufferPtr;
    public long bufferMemoryPtr;

    @Override
    public void free() {
        VK13.vkDestroyBuffer(VulkanGlobals.getLogicalDevice(), bufferPtr, null);
        VK13.vkFreeMemory(VulkanGlobals.getLogicalDevice(), bufferMemoryPtr, null);
    }

    public long bufferPointer() {
        return bufferPtr;
    }

    public void bufferPointer(long ptr) {
        bufferPtr = ptr;
    }

    public long bufferMemoryPointer() {
        return bufferMemoryPtr;
    }

    public void bufferMemoryPointer(long ptr) {
        bufferMemoryPtr = ptr;
    }
}
