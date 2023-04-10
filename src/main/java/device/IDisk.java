package device;

interface IDisk {
    /**
     * 读取一个指定扇区的数据。
     *
     * @param sectorIdx 扇区索引，起始索引为0，终止索引为 {@code sectorCount()-1}
     * @return 扇区数据，返回的字节数组长度必须等于{@code sectorSize()}
     */
    byte[] readSector(int sectorIdx);

    /**
     * 写一个指定扇区。
     *
     * @param sectorIdx 扇区索引，起始索引为0，终止索引为 {@code sectorCount()-1}
     * @param sectorData 待写入的数据. 长度必须等于{@code sectorSize()}
     */
    void writeSector(int sectorIdx, byte[] sectorData);

    /**
     * 磁盘每个扇区的大小，固定为512字节
     */
    default int sectorSize() {
        return 512;
    }

    /**
     * 磁盘扇区数量，固定为 2G/512
     */
    default long sectorCount() {
        return 2 * 1024 * 1024 * 1024L / sectorSize();
    }
}
