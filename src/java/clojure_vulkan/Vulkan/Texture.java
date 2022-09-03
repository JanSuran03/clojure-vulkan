package clojure_vulkan.Vulkan;

import org.lwjgl.system.NativeResource;
import org.lwjgl.vulkan.VK13;

public class Texture implements NativeResource {
    public Texture() {
    }

    private long imagePointer = 0L;
    private long imageMemoryPointer = 0L;
    private long textureImageViewPointer = 0L;
    private long textureSamplerPointer = 0L;

    public long imagePointer() {
        return imagePointer;
    }

    public void imagePointer(long ptr) {
        imagePointer = ptr;
    }

    public long imageMemoryPointer() {
        return imageMemoryPointer;
    }

    public void imageMemoryPointer(long ptr) {
        imageMemoryPointer = ptr;
    }

    public long textureImageViewPointer() {
        return textureImageViewPointer;
    }

    public void textureImageViewPointer(long imageView) {
        textureImageViewPointer = imageView;
    }

    public long textureSamplerPointer() {
        return textureSamplerPointer;
    }

    public void textureSamplerPointer(long ptr) {
        textureSamplerPointer = ptr;
    }

    @Override
    public void free() {
        VK13.vkDestroyImage(VulkanGlobals.getLogicalDevice(), imagePointer, null);
        imagePointer = 0L;
        VK13.vkFreeMemory(VulkanGlobals.getLogicalDevice(), imageMemoryPointer, null);
        imageMemoryPointer = 0L;
        VK13.vkDestroySampler(VulkanGlobals.getLogicalDevice(), textureSamplerPointer, null);
        textureSamplerPointer = 0L;
        VK13.vkDestroyImageView(VulkanGlobals.getLogicalDevice(), textureImageViewPointer, null);
        textureImageViewPointer = 0L;
    }
}
