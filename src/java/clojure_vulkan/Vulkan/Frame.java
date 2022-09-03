package clojure_vulkan.Vulkan;

import clojure.lang.IPersistentVector;
import clojure.lang.ISeq;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.vulkan.VK13;

import java.util.Vector;

public class Frame {
    public Frame() {
    }

    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    public static Vector<Frame> FRAMES;
    public static boolean isFrameBufferResized = false;
    private static int currentFrameCounter = 0;

    private long imageAvailableSemaphorePointer;
    private long renderFinishedSemaphorePointer;
    private long inFlightFencePointer;

    public static long getCurrentFrameCounter() {
        return currentFrameCounter;
    }

    public static Frame currentFrame() {
        return FRAMES.elementAt(currentFrameCounter);
    }

    public static void nextFrame() {
        double newTime = GLFW.glfwGetTime();
        VulkanGlobals.deltaTime = newTime - VulkanGlobals.oldTime;
        VulkanGlobals.oldTime = newTime;
        currentFrameCounter = (currentFrameCounter + 1) % MAX_FRAMES_IN_FLIGHT;
    }

    public long imageAvailableSemaphorePointer() {
        return imageAvailableSemaphorePointer;
    }

    public void imageAvailableSemaphorePointer(long ptr) {
        imageAvailableSemaphorePointer = ptr;
    }

    public long renderFinishedSemaphorePointer() {
        return renderFinishedSemaphorePointer;
    }

    public void renderFinishedSemaphorePointer(long ptr) {
        renderFinishedSemaphorePointer = ptr;
    }

    public long inFlightFencePointer() {
        return inFlightFencePointer;

    }


    public void inFlightFencePointer(long ptr) {
        inFlightFencePointer = ptr;
    }

    public static void createFrames(IPersistentVector framesVec) {
        Vector<Frame> temp = new Vector<>();
        for (ISeq frames = framesVec.seq(); frames != null; frames = frames.next())
            temp.add((Frame) frames.first());
        FRAMES = temp;
    }

    public static void setFrameBufferResized(boolean isResized) {
        isFrameBufferResized = isResized;
    }

    public static void cleanup() {
        for (Frame f : FRAMES) {
            VK13.vkDestroySemaphore(VulkanGlobals.getLogicalDevice(), f.imageAvailableSemaphorePointer, null);
            VK13.vkDestroySemaphore(VulkanGlobals.getLogicalDevice(), f.renderFinishedSemaphorePointer, null);
            VK13.vkDestroyFence(VulkanGlobals.getLogicalDevice(), f.inFlightFencePointer, null);
            FRAMES = null;
            currentFrameCounter = 0;
        }
    }
}
