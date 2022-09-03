package clojure_vulkan.Vulkan;

import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VK13;
import org.lwjgl.vulkan.VkBufferCreateInfo;

public class Buffer implements NativeResource {
    public Buffer() {
    }

    private long bufferPtr;
    private long bufferMemoryPtr;
    private VkBufferCreateInfo bufferCreateInfo;

    @Override
    public void free() {
        bufferCreateInfo = null;
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

    public VkBufferCreateInfo bufferCreateInfo() {
        if (bufferCreateInfo == null) {
            throw new RuntimeException("Failed to get the value of bufferCreateInfo.");
        }
        return bufferCreateInfo;
    }

    public void bufferCreateInfo(VkBufferCreateInfo createInfo) {
        bufferCreateInfo = createInfo;
    }
}
