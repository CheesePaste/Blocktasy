package cheesepaste.blocktasy.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static cheesepaste.blocktasy.Blocktasy.MOD_ID;

public class ModItems {

    public static final Item PUZZLE_WAND = new IronBlockWandItem(
            new Item.Settings()
                    .maxCount(1)
                    .maxDamage(500)
    );

    public static void initialize() {
        Registry.register(Registries.ITEM,
                Identifier.of(MOD_ID, "iron-blockwand"),
                PUZZLE_WAND);
    }
}
