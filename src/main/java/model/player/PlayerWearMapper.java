package model.player;

import com.google.gson.*;
import game.guild.Guild;
import template.Option;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PlayerWearMapper implements JsonDeserializer<PlayerWear> {

    @Override
    public PlayerWear deserialize(JsonElement json,
                                  Type typeOfT,
                                  JsonDeserializationContext context) {

        JsonArray array = json.getAsJsonArray();

        List<Option> pairs = new ArrayList<>();
        JsonArray nestedPairs = array.get(8).getAsJsonArray();

        for (JsonElement element : nestedPairs) {
            JsonArray pairArray = element.getAsJsonArray();
            int id = pairArray.get(0).getAsInt();
            int value = pairArray.get(1).getAsInt();
            pairs.add(new Option(id, value));
        }



        return new PlayerWear(
                array.get(0).getAsShort(),
                array.get(1).getAsByte(),
                array.get(2).getAsByte(),
                array.get(3).getAsShort(),
                array.get(4).getAsShort(),
                array.get(5).getAsByte(),
                array.get(6).getAsByte(),
                array.get(7).getAsByte(),
                pairs,
                array.get(9).getAsByte(),
                array.get(10).getAsByte(),
                array.get(11).getAsLong()
        );
    }
}
