package utils;

import fs.protocol.FAT16X;

public class FsHelper {

    /**
     * 判断是否是空数据区，规则：如果全部是0x00的话，则说明这片区域没写过值
     */
    public static boolean isEmpty(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if(data[i] != FAT16X.EMPTY_BYTE) {
                return false;
            }
        }
        return true;
    }
}
