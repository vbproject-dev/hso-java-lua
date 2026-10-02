package client.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Message implements AutoCloseable {
	public byte cmd;
	private ByteArrayOutputStream os;
	private DataOutputStream dos;
	private ByteArrayInputStream is;
	private DataInputStream dis;

	private byte[] data;



	public Message(int cmd) {
		this.cmd = (byte) cmd;
		this.os = new ByteArrayOutputStream();
		this.dos = new DataOutputStream(os);
	}

	public Message(byte cmd, byte[] data) {
		this.cmd = cmd;
		this.data = data.clone();
		this.is = new ByteArrayInputStream(data);
		this.dis = new DataInputStream(is);
	}

	public DataOutputStream writer() {
		return dos;
	}

	public DataInputStream reader() {
		return dis;
	}

	public DataInputStream copyReader() {
		return new DataInputStream(
				new ByteArrayInputStream(data)
		);
	}

	public byte[] getData() {
		return  os.toByteArray();
	}

	public void cleanup() throws IOException {
		if (os != null) {
			os.close();
		}
		if (is != null) {
			is.close();
		}
		if (dis != null) {
			dis.close();
		}
		if (dos != null) {
			dos.close();
		}
	}

	public int getCmd() {return cmd;}

    @Override
    public void close() throws Exception {
        cleanup();
    }
}
