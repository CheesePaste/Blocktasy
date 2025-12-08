package cheesepaste.blocktasy.component.ability;


import cheesepaste.blocktasy.component.EntityComponents;
import cheesepaste.blocktasy.entity.BaseBlockEntity;
import cheesepaste.blocktasy.entity.FollowingEntity;

public abstract class BlockAbilityComponent extends EntityComponents {
    public BlockAbilityComponent(BaseBlockEntity baseBlockEntity) {
        super(baseBlockEntity);
    }
    FollowingEntity blockentity;
    public abstract void OnSpawn();
    abstract ActionResult Attack();
    abstract void OnDestroy();
}