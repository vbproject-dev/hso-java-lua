package template;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DataEffect {
    private int id;
    private byte[] data;
    transient private int dx;
    transient private int dy;

}
