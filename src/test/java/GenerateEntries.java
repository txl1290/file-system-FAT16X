import dirven.DiskDriven;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import protocol.FAT16X;
import utils.Transfer;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试边界情况，生成测试数据
 */
public class GenerateEntries {

    public static final String CHAR_COLLECTION = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01#$%&'()-@";

    public static final String TEST_DIR = "test";

    @BeforeClass
    public static void init() {
        //DiskDriven.format();
    }

    /**
     * 测试根目录满的情况
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFullRootEntries() {
        DiskDriven.makeDirectory(DiskDriven.getAbsolutePath(TEST_DIR));
        for (int i = 0; i < 1008; i++) {
            //随机生成文件名
            String fileName = getRandomString(8, CHAR_COLLECTION);
            if(i % 2 == 0) {
                DiskDriven.makeDirectory(DiskDriven.getAbsolutePath(fileName));
            } else {
                DiskDriven.createFile(DiskDriven.getAbsolutePath(fileName), 0);
            }
        }
    }

    private String getRandomString(int length, String charCollection) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * charCollection.length());
            builder.append(charCollection.charAt(index));
        }
        return builder.toString();
    }

    /**
     * 生成一个大文件，跨cluster
     */
    @Test
    public void generateBigFile() {
        String fileName = TEST_DIR + "/bigFile.tmp";
        FAT16X.DirectoryEntry file = DiskDriven.findEntry(DiskDriven.getAbsolutePath(fileName));
        if(file == null) {
            file = DiskDriven.createFile(DiskDriven.getAbsolutePath(fileName), 0);
        }

        // 大文件写入额外申请簇
        byte[] content = new byte[DiskDriven.getDisk().clusterSize() * 2];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (Math.random() * 256);
        }
        DiskDriven.writeFileContent(fileName, content);

        int startCluster = Transfer.short2Int(file.getStartingCluster());
        List<Integer> usedClusters = new ArrayList<>();
        short[] fat = DiskDriven.getDisk().getFs().getFatTable();
        do {
            usedClusters.add(startCluster);
            startCluster = fat[startCluster];
        } while (startCluster != FAT16X.FAT16X_END_OF_FILE);

        int lastCluster = usedClusters.get(usedClusters.size() - 1);

        Assert.assertEquals(2, usedClusters.size());

        // 跨cluster读取
        file = DiskDriven.findEntry(DiskDriven.getAbsolutePath(fileName));
        byte[] read = DiskDriven.readFileContent(file);
        Assert.assertArrayEquals(content, read);

        // 释放簇
        DiskDriven.writeFileContent(fileName, "hello world".getBytes());

        startCluster = Transfer.short2Int(file.getStartingCluster());
        usedClusters = new ArrayList<>();
        do {
            usedClusters.add(startCluster);
            startCluster = fat[startCluster];
        } while (startCluster != FAT16X.FAT16X_END_OF_FILE);

        Assert.assertEquals(1, usedClusters.size());
        Assert.assertEquals(fat[lastCluster], FAT16X.FAT16X_FREE_CLUSTER);
    }

    /**
     * 测试磁盘满的情况(很慢)
     */
    @Test(expected = IllegalStateException.class)
    public void testFullDisk() {
        for (int i = 0; i <= DiskDriven.getDisk().clusterCount(); i++) {
            //随机生成文件名
            String fileName = getRandomString(8, CHAR_COLLECTION);
            if(i % 2 == 0) {
                DiskDriven.makeDirectory(DiskDriven.getAbsolutePath(TEST_DIR + "/" + fileName));
            } else {
                DiskDriven.createFile(DiskDriven.getAbsolutePath(TEST_DIR + "/" + fileName), 0);
            }
        }
    }

    /**
     * 测试512M文件
     */
    @Test
    public void test512MFile() {
        byte content[] = new byte[512 * 1024 * 1024];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (Math.random() * 256);
        }
        DiskDriven.writeFileContent(TEST_DIR + "/hugeFile.tmp", content);
    }
}
