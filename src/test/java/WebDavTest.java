import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 描述
 */
public class WebDavTest {

    private static final String WEBDAV_URL = "http://10.26.27.48:9001/";

    @Test
    public void testWebDav() throws IOException {
        Sardine client = SardineFactory.begin("username", "123456");
        List<DavResource> resources = client.list(WEBDAV_URL);
        for (DavResource res : resources) {
            if(!res.isDirectory()) {
                InputStream input = client.get(WEBDAV_URL + res.getPath());
                Long length = res.getContentLength();
                byte[] buf = new byte[length.intValue()];
                input.read(buf);
                System.out.println(res.getPath() + "-------" + new String(buf));
            }
        }
    }

    @Test
    public void testAddFile() throws IOException {
        Sardine client = SardineFactory.begin("username", "123456");
        //if(!client.exists(WEBDAV_URL + "txl")) {
        //    client.createDirectory(WEBDAV_URL + "txl");
        //}

        client.put(WEBDAV_URL + "test.txt", "这是一个测试文件".getBytes());
    }
}
