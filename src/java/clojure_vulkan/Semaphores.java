package clojure_vulkan;

public class Semaphores {
    private long imageAvailableSemaphorePointer;
    private long renderFinishedSemaphorePointer;
    private long inFlightFencePointer;

    public Semaphores() {
        imageAvailableSemaphorePointer = 0;
        renderFinishedSemaphorePointer = 0;
        inFlightFencePointer = 0;
    }

    public Semaphores(long imageAvailableSemaphorePointer, long renderFinishedSemaphorePointer, long inFlightFencePointer) {
        this.imageAvailableSemaphorePointer = imageAvailableSemaphorePointer;
        this.renderFinishedSemaphorePointer = renderFinishedSemaphorePointer;
        this.inFlightFencePointer = inFlightFencePointer;
    }

    public long getImageAvailableSemaphorePointer() {
        return imageAvailableSemaphorePointer;
    }

    public long getRenderFinishedSemaphorePointer() {
        return renderFinishedSemaphorePointer;
    }

    public long getInFlightFencePointer() {
        return inFlightFencePointer;
    }

    public Semaphores setImageAvailableSemaphorePointer(long newValue) {
        imageAvailableSemaphorePointer = newValue;
        return this;
    }

    public Semaphores setRenderFinishedSemaphorePointer(long newValue) {
        renderFinishedSemaphorePointer = newValue;
        return this;
    }

    public Semaphores setInFlightFencePointer(long newValue) {
        inFlightFencePointer = newValue;
        return this;
    }
}
