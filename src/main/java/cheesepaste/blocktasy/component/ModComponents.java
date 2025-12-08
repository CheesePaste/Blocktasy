package cheesepaste.blocktasy.component;

import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModComponents {
    // 定义魔杖模式组件
    public static final ComponentType<WandModeComponent> WAND_MODE =
            ComponentType.<WandModeComponent>builder()
                    .codec(WandModeComponent.CODEC)
                    .packetCodec(WandModeComponent.PACKET_CODEC)
                    .build();

    public static void register() {
        Registry.register(Registries.DATA_COMPONENT_TYPE, "blocktasy:wand_mode", WAND_MODE);
    }

}

