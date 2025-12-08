package cheesepaste.blocktasy.entity;


import cheesepaste.blocktasy.Blocktasy;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<FollowingEntity> FollowingEntity = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(Blocktasy.MOD_ID, "following_block"),
            EntityType.Builder.<FollowingEntity>create(FollowingEntity::new, SpawnGroup.MISC)
                    .dimensions(0.98f,0.98f)
                    .maxTrackingRange(64) // 注意：方法名变了
                    .trackingTickInterval(1) // 注意：方法名变了
                    .alwaysUpdateVelocity(true)
                    .build()
    );

    public static void initialize() {
        Blocktasy.LOGGER.info("Registering mod entities for {}", Blocktasy.MOD_ID);
    }
}

