package model.map;

import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

@Data
public class TileData {
    private int imageId;
    private int width;
    private int height;
    private byte[] data;


    public byte[] toBytes() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeByte(width);
            dos.writeByte(height);
            dos.writeByte(imageId);
            dos.write(data);

            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
