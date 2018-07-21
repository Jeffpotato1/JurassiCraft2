package org.jurassicraft.server.block.entity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.server.api.Hybrid;
import org.jurassicraft.server.container.DNAExtractorContainer;
import org.jurassicraft.server.dinosaur.Dinosaur;
import org.jurassicraft.server.registries.JurassicraftRegisteries;
import org.jurassicraft.server.genetics.DinoDNA;
import org.jurassicraft.server.genetics.GeneticsHelper;
import org.jurassicraft.server.genetics.PlantDNA;
import org.jurassicraft.server.item.ItemHandler;
import org.jurassicraft.server.plant.Plant;
import org.jurassicraft.server.plant.PlantHandler;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class DNAExtractorBlockEntity extends MachineBaseBlockEntityOLD {
    private static final int[] INPUTS = new int[]{0, 1};
    private static final int[] OUTPUTS = new int[]{2, 3, 4, 5};

    private NonNullList<ItemStack> slots = NonNullList.withSize(6, ItemStack.EMPTY);

    @Override
    protected int getProcess(int slot) {
        return 0;
    }

    @Override
    protected boolean canProcess(int process) {
        ItemStack extraction = this.slots.get(0);
        ItemStack storage = this.slots.get(1);
        if (storage.getItem() == ItemHandler.STORAGE_DISC && (extraction.getItem() == ItemHandler.AMBER || extraction.getItem() == ItemHandler.SEA_LAMPREY || extraction.getItem() == ItemHandler.DINOSAUR_MEAT) && (storage.getTagCompound() == null || !storage.getTagCompound().hasKey("Genetics"))) {
            for (int i = 2; i < 6; i++) {
                if (this.slots.get(i).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void processItem(int process) {
        if (this.canProcess(process)) {
            Random rand = this.world.rand;
            ItemStack input = this.slots.get(0);

            ItemStack disc = ItemStack.EMPTY;

            Item item = input.getItem();

            if (item == ItemHandler.AMBER || item == ItemHandler.SEA_LAMPREY) {
                if (input.getItemDamage() == 0) {
                    Optional<Dinosaur> dinosaur = JurassicraftRegisteries.DINOSAUR_REGISTRY.getValuesCollection().stream()
                            .filter(dino -> !(dino instanceof Hybrid) && (item != ItemHandler.AMBER) == (dino.homeType == Dinosaur.DinosaurHomeType.MARINE))
                            .findAny();

                    if(dinosaur.isPresent()) {
                        disc = new ItemStack(ItemHandler.STORAGE_DISC);
                        int quality = (rand.nextInt(10) + 1) * 5;
                        if (rand.nextInt(2) > 0) {
                            quality += 50;
                        }

                        DinoDNA dna = new DinoDNA(dinosaur.get(), quality, GeneticsHelper.randomGenetics(rand));

                        NBTTagCompound nbt = new NBTTagCompound();
                        dna.writeToNBT(nbt);

                        disc.setTagCompound(nbt);
                    }
                } else if (input.getItemDamage() == 1) {
                    List<Plant> possiblePlants = PlantHandler.getPrehistoricPlantsAndTrees();

                    disc = new ItemStack(ItemHandler.STORAGE_DISC);

                    int quality = (rand.nextInt(10) + 1) * 5;

                    if (rand.nextInt(2) > 0) {
                        quality += 50;
                    }

                    PlantDNA dna = new PlantDNA(possiblePlants.get(rand.nextInt(possiblePlants.size())), quality);

                    NBTTagCompound nbt = new NBTTagCompound();
                    dna.writeToNBT(nbt);

                    disc.setTagCompound(nbt);
                }
            } else if (item == ItemHandler.DINOSAUR_MEAT) {
                disc = new ItemStack(ItemHandler.STORAGE_DISC);
                disc.setTagCompound(input.getTagCompound());
            }

            int empty = this.getOutputSlot(disc);

            this.mergeStack(empty, disc);

            this.decreaseStackSize(0);
            this.decreaseStackSize(1);
        }
    }

    @Override
    protected int getMainOutput(int process) {
        return 2;
    }

    @Override
    protected int getStackProcessTime(ItemStack stack) {
        return 2000;
    }

    @Override
    protected int getProcessCount() {
        return 1;
    }

    @Override
    protected int[] getInputs() {
        return INPUTS;
    }

    @Override
    protected int[] getInputs(int process) {
        return this.getInputs();
    }

    @Override
    protected int[] getOutputs() {
        return OUTPUTS;
    }

    @Override
    protected NonNullList<ItemStack> getSlots() {
        return this.slots;
    }

    @Override
    protected void setSlots(NonNullList<ItemStack> slots) {
        this.slots = slots;
    }

    @Override
    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn) {
        return new DNAExtractorContainer(playerInventory, this);
    }

    @Override
    public String getGuiID() {
        return JurassiCraft.MODID + ":dna_extractor";
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : "container.dna_extractor";
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
