package clojure_vulkan;

public class Semaphores {
    private final long[] imageAvailableSemaphorePointers;
    private final long[] renderFinishedSemaphorePointers;
    private final long[] inFlightFencePointers;

    private int currentFrame = 0;
    public int maxFramesInFlight;

    public Semaphores(int maxFramesInFlight) {
        this.maxFramesInFlight = maxFramesInFlight;
        imageAvailableSemaphorePointers = new long[maxFramesInFlight];
        renderFinishedSemaphorePointers = new long[maxFramesInFlight];
        inFlightFencePointers = new long[maxFramesInFlight];
    }

    public long getImageAvailableSemaphorePointer(int index) {
        return imageAvailableSemaphorePointers[index];
    }

    public long getRenderFinishedSemaphorePointer(int index) {
        return renderFinishedSemaphorePointers[index];
    }

    public long getInFlightFencePointer(int index) {
        return inFlightFencePointers[index];
    }

    public Semaphores setImageAvailableSemaphorePointer(int index, long newValue) {
        imageAvailableSemaphorePointers[index] = newValue;
        return this;
    }

    public Semaphores setRenderFinishedSemaphorePointer(int index, long newValue) {
        renderFinishedSemaphorePointers[index] = newValue;
        return this;
    }

    public Semaphores setInFlightFencePointer(int index, long newValue) {
        inFlightFencePointers[index] = newValue;
        return this;
    }

    public int nextFrame() {
        return currentFrame = (currentFrame + 1) % maxFramesInFlight;
    }
}
