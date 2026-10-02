package template;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;


import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)

public class SvConfig {
    short priceSellPotion;
    short priceSellItem;
    short hesoLevel;
    short hesoColor;
    short priceSellQuest;
    short maxPriceItem;
    short priceClanIcon;
    byte priceChatWorld;
    List<PetType> petTemplate;
    int[] craftMaterial;

    // MOUNT TEMPLATE
    byte[] speed;
    byte[][] previousFrame;        // FRAME_VE_TRUOC
    byte[][] dyCharStand;         // DY_CHAR_STAND
    byte[][] dyCharMove;          // DY_CHAR_MOVE

    byte[][] dx;              // dx
    byte[][] dy;              // dy

    byte[][] moveFramesLr;         // FRAME_MOVE_LR
    byte[][] moveFramesDown;       // FRAME_MOVE_DOWN
    byte[][] moveFramesUp;

    byte[] atbCantPaint;
    byte[] petTypeMove;
    byte[] itemEquip;
    byte[] itemEquipRotate;
    short[] idBoss;
    byte[] hairNoHat;

    List<DataEffect> dataEffects;
    byte[][] effectSkill;
    byte[] flyMount;

    private String notifServer;
}
