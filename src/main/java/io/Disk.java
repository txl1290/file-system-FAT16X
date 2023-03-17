package io;

import lombok.Data;
import protocol.FAT16X;
import utils.FsHelper;
import utils.Transfer;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class Disk implements IDisk {

    FAT16X fs;

    static final int FAT_START_CLUSTER_IDX = 1;

    public Disk() {
        fs = new FAT16X();
        File file = new File("disk");
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        fs.setDataRegion(file);
        init();
    }

    public void init() {
        // 读取磁盘非数据区信息
        byte[] firstSector = readSector(0);
        if(!FsHelper.isEmpty(firstSector)) {
            FAT16X.BootSector bootSector = new FAT16X.BootSector(firstSector);
            fs.setBootSector(bootSector);
        }

        initRootDirectory();
        initFatTable();
    }

    private void initRootDirectory() {
        List<FAT16X.DirectoryEntry> entries = new ArrayList<>();
        for (int i = 0; i < fs.getBootSector().getNumberOfPossibleRootEntries(); i++) {
            byte[] sectorData = readSector(i + 1);
            if(FsHelper.isEmpty(sectorData)) {
                break;
            }
            entries.addAll(Transfer.bytesToEntries(sectorData));
        }
        fs.setRootDirectory(entries.toArray(new FAT16X.DirectoryEntry[0]));
    }

    private void initFatTable() {
        // 从磁盘读取FAT表
        int clusterCount = clusterCount();
        short[] fatTable = new short[clusterCount];
        // 第二个cluster开始是FAT区域
        int fatTableIdx = 0;
        for (int i = fatStartClusterIdx(); i < fatEndClusterIdx(); i++) {
            byte[] clusterData = readCluster(i);
            for (int j = 0; j < clusterData.length; j += 2) {
                fatTable[fatTableIdx++] = Transfer.bytes2Short(clusterData[j], clusterData[j + 1]);
            }
            if(fatTable[0] == FAT16X.EMPTY_BYTE) {
                break;
            }
        }

        // 如果FAT表为空，则初始化
        if(fatTable[0] == FAT16X.EMPTY_BYTE) {
            fatTable[0] = fs.getBootSector().getMediaDescriptor();
            // todo: check logic
            for (int i = fatStartClusterIdx(); i < fatEndClusterIdx(); i++) {
                fatTable[i] = FAT16X.FAT16X_EOC;
            }

            // 初始化副本
            for (int i = 1; i < fs.getBootSector().getNumberOfFATCopies(); i++) {
                int start = fatEndClusterIdx() + (i - 1) * (fatEndClusterIdx() - fatStartClusterIdx());
                int end = fatEndClusterIdx() + i * (fatEndClusterIdx() - fatStartClusterIdx());
                for (int j = start; j < end; j++) {
                    fatTable[j] = FAT16X.FAT16X_EOC;
                }
            }
        }
        fs.setFatTable(fatTable);
    }

    @Override
    public byte[] readSector(int sectorIdx) {
        byte[] sectorData = new byte[sectorSize()];
        try {
            if(sectorIdx >= sectorCount()) {
                throw new IllegalArgumentException("Sector index out of range: " + sectorIdx);
            }
            RandomAccessFile fw = new RandomAccessFile(fs.getDataRegion().getAbsolutePath(), "r");
            fw.seek((long) sectorIdx * sectorSize());
            fw.read(sectorData);
            fw.close();
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
            RandomAccessFile fw = new RandomAccessFile(fs.getDataRegion().getAbsolutePath(), "rw");
            fw.seek((long) sectorIdx * sectorSize());
            fw.write(sectorData);
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int sectorSize() {
        return fs.getBootSector().getBytesPerSector();
    }

    public int clusterCount() {
        return (int) (sectorCount() / fs.getBootSector().getSectorsPerCluster());
    }

    public int clusterSize() {
        return fs.getBootSector().getSectorsPerCluster() * sectorSize();
    }

    public byte[] readCluster(int clusterIdx) {
        byte[] clusterData = new byte[clusterSize()];
        int sectorIdx = clusterIdx * fs.getBootSector().getSectorsPerCluster();
        for (int i = 0; i < fs.getBootSector().getSectorsPerCluster(); i++) {
            byte[] sectorData = readSector(sectorIdx + i);
            System.arraycopy(sectorData, 0, clusterData, i * sectorSize(), sectorSize());
        }
        return clusterData;
    }

    public void writeCluster(int clusterIdx, byte[] clusterData) {
        int sectorIdx = clusterIdx * fs.getBootSector().getSectorsPerCluster();
        for (int i = 0; i < fs.getBootSector().getSectorsPerCluster(); i++) {
            byte[] sectorData = new byte[sectorSize()];
            System.arraycopy(clusterData, i * sectorSize(), sectorData, 0, sectorSize());
            writeSector(sectorIdx + i, sectorData);
        }
    }

    public void freeCluster(int clusterIdx) {
        byte[] clusterData = new byte[clusterSize()];
        Arrays.fill(clusterData, FAT16X.EMPTY_BYTE);
        writeCluster(clusterIdx, clusterData);
        fs.getFatTable()[clusterIdx] = FAT16X.FAT16X_FREE_CLUSTER;
    }

    public void clean() {
        int sectorIdx = 0;
        byte[] empty = new byte[sectorSize()];
        Arrays.fill(empty, FAT16X.EMPTY_BYTE);
        while (sectorIdx < sectorCount()) {
            writeSector(sectorIdx, empty);
            sectorIdx++;
        }
    }

    public int fatStartClusterIdx() {
        return FAT_START_CLUSTER_IDX;
    }

    public int fatEndClusterIdx() {
        return fatStartClusterIdx() + fs.getBootSector().getSectorsPerFAT() / fs.getBootSector().getSectorsPerCluster();
    }
}
