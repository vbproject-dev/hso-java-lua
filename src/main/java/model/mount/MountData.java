package model.mount;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class MountData {
    final int id;
    final String name;
    final int type;
    final int part;
}