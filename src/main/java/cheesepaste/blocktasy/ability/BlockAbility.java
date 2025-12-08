package cheesepaste.blocktasy.ability;


import cheesepaste.blocktasy.entity.FollowingEntity;

public abstract class BlockAbility {
    public BlockAbility(FollowingEntity blockentity)
    {
        this.blockentity=blockentity;
    }
    FollowingEntity blockentity;
    abstract void OnSpawn();
    abstract ActionResult Attack();
    abstract void Update();
    abstract void OnDestroy();
}