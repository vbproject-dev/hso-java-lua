package model.mount;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class MountFrames {
    int id;
    byte speed;
    byte[] framePrevious;
    byte[] dyStand;
    byte[] dyMove;
    byte[] dx;
    byte[] dy;
    byte[] moveFrameLr;
    byte[] moveFrameDown;
    byte[] moveFrameUp;
}