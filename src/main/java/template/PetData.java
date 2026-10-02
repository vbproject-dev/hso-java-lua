package template;




import lombok.Data;


import java.util.ArrayList;
import java.util.List;

@Data
public class PetData {
    private int id;
    private String name;
    private List<Byte> image;
    private int icon;
    protected int frameCount;
    private int color;
    private int maxGrow;
    private int hatchTime;
    private List<PetOption> options = new ArrayList<>();
    private int typeMove;
}
