package device;

import lombok.Data;

import java.io.RandomAccessFile;

@Data
public class Disk implements IDisk {

    RandomAccessFile fw;

    public Disk(RandomAccessFile fw) {
        this.fw = fw;
    }

    @Override
    public byte[] readSector(int sectorIdx) {
        byte[] sectorData = new byte[sectorSize()];
        try {
            if(sectorIdx >= sectorCount()) {
                throw new IllegalArgumentException("Sector index out of range: " + sectorIdx);
            }
            fw.seek((long) sectorIdx * sectorSize());
            fw.read(sectorData);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sectorData;
    }

    @Override
    public void writeSector(int sectorIdx, byte[] sectorData) {
        try {
            if(sectorIdx >= sectorCount()) {
                throw new IllegalArgumentException("Sector index out of range: " + sectorIdx);
            }
            fw.seek((long) sectorIdx * sectorSize());
            fw.write(sectorData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
