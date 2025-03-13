import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class ServerTest {

    Socket sock;
    OutputStream out;
    ObjectOutputStream os;
    DataInputStream in;

    @org.junit.Before
    public void setUp() throws Exception {
        sock = new Socket("localhost", 8888);
        out = sock.getOutputStream();
        os = new ObjectOutputStream(out);
        in = new DataInputStream(sock.getInputStream());
    }

    @org.junit.After
    public void close() throws Exception {
        if (out != null) out.close();
        if (sock != null) sock.close();
    }

    @Test
    public void testCharCount() throws IOException {
        JSONObject req = new JSONObject();
        req.put("type", "charcount");
        req.put("findchar", false);
        req.put("count", "hello");

        os.writeObject(req.toString());
        os.flush();

        String i = in.readUTF();
        JSONObject res = new JSONObject(i);

        assertTrue(res.getBoolean("ok"));
        assertEquals(5, res.getInt("result"));
    }

    @Test
    public void testCharCountWithFind() throws IOException {
        JSONObject req = new JSONObject();
        req.put("type", "charcount");
        req.put("findchar", true);
        req.put("find", "l");
        req.put("count", "hello");

        os.writeObject(req.toString());
        os.flush();

        String i = in.readUTF();
        JSONObject res = new JSONObject(i);

        assertTrue(res.getBoolean("ok"));
        assertEquals(2, res.getInt("result"));
    }

    @Test
    public void testInventoryAdd() throws IOException {
        JSONObject req = new JSONObject();
        req.put("type", "inventory");
        req.put("task", "add");
        req.put("productName", "Laptop");
        req.put("quantity", 5);

        os.writeObject(req.toString());
        os.flush();

        String i = in.readUTF();
        JSONObject res = new JSONObject(i);

        assertTrue(res.getBoolean("ok"));
    }

    @Test
    public void testInventoryView() throws IOException {
        JSONObject req = new JSONObject();
        req.put("type", "inventory");
        req.put("task", "view");

        os.writeObject(req.toString());
        os.flush();

        String i = in.readUTF();
        JSONObject res = new JSONObject(i);

        assertTrue(res.getBoolean("ok"));
        assertTrue(res.has("inventory"));
    }

    @Test
    public void testInventoryBuy() throws IOException {
        JSONObject addReq = new JSONObject();
        addReq.put("type", "inventory");
        addReq.put("task", "add");
        addReq.put("productName", "Laptop");
        addReq.put("quantity", 5);
        os.writeObject(addReq.toString());
        os.flush();
        in.readUTF();

        JSONObject buyReq = new JSONObject();
        buyReq.put("type", "inventory");
        buyReq.put("task", "buy");
        buyReq.put("productName", "Laptop");
        buyReq.put("quantity", 2);
        os.writeObject(buyReq.toString());
        os.flush();

        String i = in.readUTF();
        JSONObject res = new JSONObject(i);

        assertTrue(res.getBoolean("ok"));
    }

    @Test
    public void testInvalidJSON() throws IOException {
        os.writeObject("Invalid JSON");
        os.flush();

        String i = in.readUTF();
        JSONObject res = new JSONObject(i);

        assertFalse(res.getBoolean("ok"));
        assertEquals("req not JSON", res.getString("message"));
    }
}
