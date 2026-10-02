package model.map;

import lombok.Data;

/**
 * Model untuk tabel map_drop_config.
 * Mengatur drop item per map langsung dari database.
 *
 * Tabel SQL:
 * CREATE TABLE `map_drop_config` (
 *   `id`           INT AUTO_INCREMENT PRIMARY KEY,
 *   `map_id`       TINYINT NOT NULL COMMENT 'ID map: 111,112,113,114 dst',
 *   `item_id`      SMALLINT NOT NULL,
 *   `item_type`    TINYINT NOT NULL COMMENT '3=equipment 4=potion 7=material',
 *   `drop_chance`  INT NOT NULL DEFAULT 50 COMMENT 'peluang dari 300',
 *   `min_quantity` INT NOT NULL DEFAULT 1,
 *   `max_quantity` INT NOT NULL DEFAULT 1,
 *   `color`        TINYINT NOT NULL DEFAULT 0 COMMENT '0=putih 1=hijau 2=biru 3=ungu 4=emas',
 *   `active`       TINYINT NOT NULL DEFAULT 1,
 *   INDEX `idx_map_id` (`map_id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
 */
@Data
public class MapDropConfig {
    private int id;
    private byte mapId;
    private int itemId;
    private byte itemType;   // 3=equipment, 4=potion, 7=material/craft
    private int dropChance;  // peluang dari 300 (misal 60 = 60/300 = 20%)
    private int minQuantity;
    private int maxQuantity;
    private byte color;      // 0=putih, 1=hijau, 2=biru, 3=ungu, 4=emas
}
