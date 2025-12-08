package cheesepaste.blocktasy.ability;

import cheesepaste.blocktasy.Blocktasy;
import cheesepaste.blocktasy.entity.FollowingEntity;
import net.minecraft.block.BlockState;

public class DefaultBlockAbility extends BlockAbility{

    public DefaultBlockAbility(FollowingEntity blockentity) {
        super(blockentity);
    }

    @Override
    void OnSpawn() {
        BlockAbility ability=Blocktasy.abilityMap.get(blockentity.getBlockState());
        if(ability!=null)
        {
            blockentity.ability=ability;
            ability.OnSpawn();
        }
    }

    @Override
    ActionResult Attack() {
        return ActionResult.Success;
    }

    @Override
    void Update() {

    }

    @Override
    void OnDestroy() {

    }
}
