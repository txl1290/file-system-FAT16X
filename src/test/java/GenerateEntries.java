import fs.fat.FatFileSystem;
import fs.fat.Fd;
import fs.io.File;
import fs.io.FileInputStream;
import fs.io.FileOutputStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import utils.Transfer;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试边界情况，生成测试数据
 */
public class GenerateEntries {

    public static final String CHAR_COLLECTION = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz01#$%&'()-@";

    public static final String ROOT = "/";

    public static final String TEST_DIR = "test";

    static FatFileSystem fs = new FatFileSystem();

    @BeforeClass
    public static void init() {
        fs.format();
    }

    /**
     * 测试根目录满的情况
     */
    @Test(expected = IllegalStateException.class)
    public void testFullRootEntries() {
        File testDir = new File(ROOT + TEST_DIR, true, false);
        testDir.mkdir();
        for (int i = 0; i < 1008; i++) {
            //随机生成文件名
            String fileName = getRandomString(8, CHAR_COLLECTION);
            if(i % 2 == 0) {
                File dir = new File(ROOT + fileName, true, false);
                dir.mkdir();
            } else {
                File file = new File(ROOT + fileName);
                file.create();
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
        File testDir = new File(ROOT + TEST_DIR, true, false);
        if(!testDir.exist()) {
            testDir.mkdir();
        }

        String fileName = ROOT + TEST_DIR + "/bigFile.tmp";
        File file = new File(fileName);
        if(!file.exist()) {
            file.create();
        }
        FileOutputStream out = new FileOutputStream(file);

        // 大文件写入额外申请簇
        byte[] content = new byte[65536];
        for (int i = 0; i < content.length; i++) {
            content[i] = getRandomString(1, CHAR_COLLECTION).getBytes()[0];
        }
        String write = new String(content);
        out.write(write);
        out.close();

        Fd fd = File.fs.open(fileName);
        int startCluster = Transfer.short2Int(fd.getEntry().getStartingCluster());
        List<Integer> usedClusters = new ArrayList<>();
        while (startCluster != -1) {
            usedClusters.add(startCluster);
            startCluster = File.fs.getNextCluster(startCluster);
        }

        Assert.assertEquals(2, usedClusters.size());

        // 跨cluster读取
        FileInputStream in = new FileInputStream(file);
        String read = in.read();
        in.close();
        Assert.assertEquals(write, read);

        // 释放簇
        FileOutputStream out2 = new FileOutputStream(file);
        out2.write("hello world");
        out2.close();

        startCluster = Transfer.short2Int(fd.getEntry().getStartingCluster());
        usedClusters = new ArrayList<>();
        while (startCluster != -1) {
            usedClusters.add(startCluster);
            startCluster = File.fs.getNextCluster(startCluster);
        }

        Assert.assertEquals(1, usedClusters.size());
    }

    @Test
    public void test256MFile() {
        File testDir = new File(ROOT + TEST_DIR, true, false);
        if(!testDir.exist()) {
            testDir.mkdir();
        }

        String fileName = ROOT + TEST_DIR + "/bf.tmp";
        test256MFile(fileName);
    }

    /**
     * 测试256M文件
     */
    private void test256MFile(String fileName) {
        byte content[] = new byte[256 * 1024 * 1024];
        for (int i = 0; i < content.length; i++) {
            content[i] = getRandomString(1, CHAR_COLLECTION).getBytes()[0];
        }
        File file = new File(fileName);
        if(!file.exist()) {
            file.create();
        }
        FileOutputStream out = new FileOutputStream(file);
        out.write(new String(content));
        out.close();
    }
}
