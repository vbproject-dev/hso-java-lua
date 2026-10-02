package model.item;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor(force = true)
public class Item {
    private int itemId;
    private int category;
    private int quantity;
    private int duration;
}
