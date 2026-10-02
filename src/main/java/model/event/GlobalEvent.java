package model.event;

import game.items.models.RequiredItem;

import lombok.Data;
import lombok.NoArgsConstructor;
import model.item.Reward;

import java.time.LocalDateTime;
import java.util.List;

@Data

@NoArgsConstructor(force = true)
public class GlobalEvent {
    final int id;
    final String name;
    final int npcId;
    final LocalDateTime startTime;
    final LocalDateTime endTime;
    final List<RequiredItem> requiredItems;
    final Reward reward;
    final String guide;

}
