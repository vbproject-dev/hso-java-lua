package core;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Notification {
    private int id;
    private String content;
    private int type;
    private int delay;

}
