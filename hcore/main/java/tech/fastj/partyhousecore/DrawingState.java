package tech.fastj.partyhousecore;

import java.util.BitSet;

public class DrawingState {

    private final BitSet pixels;
    boolean needsUpdate;
    private byte[] pixelStore;

    public DrawingState() {
        pixels = new BitSet(160000);
        needsUpdate = true;
    }

    public void setPixel(int x, int y) {
        pixels.set(x * y);
    }

    public byte[] getPixels() {
        if (needsUpdate) {
            pixelStore = pixels.toByteArray();
        }

        return pixelStore;
    }
}
