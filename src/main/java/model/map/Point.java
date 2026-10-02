package model.map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Point {
    private int id;
    private int x;
    private int y;


    public static byte[] toBytes(List<Point> points) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeShort(points.size());
            for (Point p : points) {
                dos.writeShort(p.getId());
                dos.writeShort(p.getX());
                dos.writeShort(p.getY());
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
