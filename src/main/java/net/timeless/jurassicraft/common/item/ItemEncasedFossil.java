package net.timeless.jurassicraft.common.item;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.timeless.jurassicraft.common.block.BlockEncasedFossil;
import net.timeless.jurassicraft.common.dinosaur.Dinosaur;
import net.timeless.jurassicraft.common.entity.base.JCEntityRegistry;
import net.timeless.jurassicraft.common.lang.AdvLang;
import net.timeless.jurassicraft.common.period.EnumTimePeriod;

public class ItemEncasedFossil extends ItemBlock
{
    public ItemEncasedFossil(Block block)
    {
        super(block);
        this.setMaxDamage(0);
        this.setHasSubtypes(true);
        this.setUnlocalizedName("encased_fossil");
    }

    public String getItemStackDisplayName(ItemStack stack)
    {
        Dinosaur dinosaur = ((BlockEncasedFossil) block).getDinosaur(stack.getMetadata());

        return new AdvLang("tile.encased_fossil.name").withProperty("period", "period." + dinosaur.getPeriod().getName() + ".name").build();
    }

    @Override
    public int getMetadata(int metadata)
    {
        return metadata;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        EnumTimePeriod timePeriod = JCEntityRegistry.getDinosaurById(stack.getMetadata()).getPeriod();
        return super.getUnlocalizedName() + "." + timePeriod.getName();
    }
}