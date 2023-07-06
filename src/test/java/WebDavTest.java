import com.github.sardine.DavResource;
import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import org.junit.Test;

import java.io.ByteArrayInputStream;
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
                input.close();
            }
        }
    }

    @Test
    public void testAddFile() throws IOException {
        Sardine client = SardineFactory.begin("username", "123456");
        if(!client.exists(WEBDAV_URL + "txl/")) {
            client.createDirectory(WEBDAV_URL + "txl");
        }

        ByteArrayInputStream bis = new ByteArrayInputStream("test".getBytes());
        client.put(WEBDAV_URL + "/txl/test.txt", bis);
    }

    @Test
    public void testFileRename() throws IOException {
        Sardine client = SardineFactory.begin("username", "123456");

        String source = WEBDAV_URL + "2.log";
        String destination = WEBDAV_URL + "txl/test.txt";
        String rename = WEBDAV_URL + "txl/rename.txt";
        if(client.exists(source) && !client.exists(destination)) {
            client.copy(source, destination, false);
        }

        if(client.exists(destination) && !client.exists(rename)) {
            client.move(destination, rename, false);
        }
    }

    @Test
    public void deleteFile() throws IOException {
        Sardine client = SardineFactory.begin("username", "123456");

        String source = WEBDAV_URL + "txl/rename.txt";

        if(client.exists(source)) {
            client.delete(source);
        }
    }

    @Test
    public void deleteDir() throws IOException {
        Sardine client = SardineFactory.begin("username", "123456");

        String source = WEBDAV_URL + "txl/";

        if(client.exists(source)) {
            client.delete(source);
        }
    }
}
