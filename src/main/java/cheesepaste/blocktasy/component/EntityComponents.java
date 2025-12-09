package cheesepaste.blocktasy.component;

import cheesepaste.blocktasy.entity.BaseBlockEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;

public abstract class  EntityComponents {
    protected BaseBlockEntity parent;

    protected EntityComponents(BaseBlockEntity parent){
        this.parent=parent;
    }

    private EntityComponents() {
    }

    public void setParent(BaseBlockEntity parent) {
        this.parent = parent;
    }

    public abstract boolean tick();
    public abstract void initDT(DataTracker.Builder builder);
    public abstract void readNBT(NbtCompound nbt);
    public abstract void writeNBT(NbtCompound nbt);
    public abstract void OnSpawn();
    public abstract void registerDataTracker();
}
