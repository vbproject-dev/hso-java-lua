package devtools;

import core.Manager;
import core.Util;
import game.items.ItemManager;
import game.mount.Mount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import model.fashion.FashionData;
import template.Item3;
import template.ItemTemplate3;
import template.PartData;
import template.PartDataLoader;
import utils.IconHelper;
import utils.IconType;
import utils.SQLHelper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

@Slf4j
public class AssetsHelper {

    @Data
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    static class MountData {
        final int id;
        final String name;
        final int type;
        final int part;

    }

    public static void main(String[] args) throws IOException {
        Manager.gI().init();

        Map<Integer, Mount> mountMap = Map.ofEntries(
                Map.entry(5214, new Mount((short) 172, (byte) 22)),
                Map.entry(5215, new Mount((short) 207, (byte) 22)),
                Map.entry(5216, new Mount((short) 145, (byte) 22)),
                Map.entry(5217, new Mount((short) 115, (byte) 22)),

                Map.entry(2226, new Mount((short) 10, (byte) 15)),
                Map.entry(5227, new Mount((short) 106, (byte) 15)),
                Map.entry(5228, new Mount((short) 107, (byte) 20)),
                Map.entry(5231, new Mount((short) -1, (byte) 5)),
                Map.entry(5232, new Mount((short) -1, (byte) 11)),
                Map.entry(5233, new Mount((short) -1, (byte) 12)),
                Map.entry(5234, new Mount((short) -1, (byte) 6)),
                Map.entry(5235, new Mount((short) -1, (byte) 7)),
                Map.entry(5236, new Mount((short) -1, (byte) 8)),
                Map.entry(5237, new Mount((short) -1, (byte) 9)),

                Map.entry(5238, new Mount((short) -1, (byte) 10)),
                Map.entry(5239, new Mount((short) 116, (byte) 20)),
                Map.entry(5240, new Mount((short) 117, (byte) 22)),
                Map.entry(5241, new Mount((short) 114, (byte) 20)),
                Map.entry(5242, new Mount((short) -1, (byte) 13)),
                Map.entry(5243, new Mount((short) 69, (byte) 20)),
                Map.entry(5244, new Mount((short) 121, (byte) 20)),
                Map.entry(5245, new Mount((short) 111, (byte) 17))
        );

        List<MountData> dataToInsert = mountMap.entrySet()
                .stream()
                .map(e -> new MountData(
                        e.getKey(),                 // item id
                        ItemTemplate3.item.get(e.getKey()).getName(),      // name (replace with real name if you have it)
                        e.getValue().getType(),     // mount type
                        e.getValue().getPart()      // mount part
                ))
                .toList();

        dataToInsert.forEach(mountData -> SQLHelper.insert("mount_data").fromModel(mountData).execute());

    }
}