package org.jurassicraft.server.entity.ai;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import org.jurassicraft.server.entity.DinosaurEntity;

import java.util.LinkedList;
import java.util.List;

public class FleeEntityAI extends EntityAIBase {
    private DinosaurEntity dinosaur;
    private List<EntityLivingBase> attackers;

    public FleeEntityAI(DinosaurEntity dinosaur) {
        this.dinosaur = dinosaur;
    }

    @Override
    public boolean shouldExecute() {
        if (this.dinosaur.ticksExisted % 10 == 0) {
            List<DinosaurEntity> entities = this.dinosaur.worldObj.getEntitiesWithinAABB(DinosaurEntity.class, this.dinosaur.getEntityBoundingBox().expand(10, 5, 10));

            this.attackers = new LinkedList<>();

            for (DinosaurEntity entity : entities) {
                if (entity != this.dinosaur && !entity.isCarcass()) {
                    for (Class<? extends EntityLivingBase> target : entity.getAttackTargets()) {
                        if (target.isAssignableFrom(this.dinosaur.getClass())) {
                            this.attackers.add(entity);
                            if (entity.getAttackTarget() == null) {
                                entity.setAttackTarget(this.dinosaur);
                            }
                            if (entity.herd != null) {
                                if (this.dinosaur.herd != null) {
                                    entity.herd.enemies.addAll(this.dinosaur.herd.members);
                                } else {
                                    entity.herd.enemies.add(this.dinosaur);
                                }
                            }
                            break;
                        }
                    }
                }
            }

            return this.attackers.size() > 0;
        }

        return false;
    }

    @Override
    public boolean continueExecuting() {
        return false;
    }

    @Override
    public void startExecuting() {
        Herd herd = this.dinosaur.herd;

        if (herd != null && this.attackers != null && this.attackers.size() > 0) {
            for (EntityLivingBase attacker : this.attackers) {
                if (!herd.enemies.contains(attacker)) {
                    herd.enemies.add(attacker);
                }
            }

            herd.fleeing = true;
        }
    }
}
