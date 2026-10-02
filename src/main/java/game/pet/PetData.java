package game.pet;

import lombok.Data;
import java.util.List;

@Data
public class PetData {
    private int id;
    private int maxGrow;
    private List<PetOption> options;
}