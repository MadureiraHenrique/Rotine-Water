package br.edu.ucsal.rotinewater;

public class WaterEntry {

    private final int volumeMl;
    private final long timestamp;

    public WaterEntry(int volumeMl, long timestamp) {
        this.volumeMl = volumeMl;
        this.timestamp = timestamp;
    }

    public int getVolumeMl() {
        return volumeMl;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
