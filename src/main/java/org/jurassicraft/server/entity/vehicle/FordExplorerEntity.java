package org.jurassicraft.server.entity.vehicle;


import java.util.List;

import javax.annotation.Nullable;

import org.jurassicraft.server.block.TourRailBlock;
import org.jurassicraft.server.entity.ai.util.InterpValue;
import org.jurassicraft.server.entity.ai.util.MathUtils;
import org.jurassicraft.server.entity.vehicle.util.WheelParticleData;
import org.jurassicraft.server.item.ItemHandler;

import com.google.common.collect.Lists;

import net.minecraft.block.BlockRailBase;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class FordExplorerEntity extends CarEntity {

    public boolean prevOnRails;
    public boolean onRails;
    private boolean lastDirBackwards;
    
    public final MinecartLogic minecart = new MinecartLogic();
    
    private final InterpValue rotationYawInterp = new InterpValue(5f);
    
    /* =================================== CAR START ===========================================*/
    
    public FordExplorerEntity(World world) {
        super(world);
    }

    @Override
    public void dropItems() {
        this.dropItem(ItemHandler.FORD_EXPLORER, 1);
    }

    @Override
    protected Seat[] createSeats() {
        Seat frontLeft = new Seat(0, 0.563F, 0.45F, 0.4F, 0.5F, 0.25F);
        Seat frontRight = new Seat(1, -0.563F, 0.45F, 0.4F, 0.5F, 0.25F);
        Seat backLeft = new Seat(2, 0.5F, 0.7F, -2.2F, 0.4F, 0.25F);
        Seat backRight = new Seat(3, -0.5F, 0.7F, -2.2F, 0.4F, 0.25F);
        return new Seat[] { frontLeft, frontRight, backLeft, backRight };
    }
    
    @Override
    protected boolean shouldRunUpdates() {
        return !onRails;
    }
    
    @Override
    public void onUpdate() {
	boolean isRails = world.getBlockState(getPosition()).getBlock() instanceof TourRailBlock;
	if(!isRails) {
	    isRails = world.getBlockState(getPosition().down()).getBlock() instanceof TourRailBlock;
	}
	
	if(onRails != isRails) {
	    if(isRails) {
		minecart.isInReverse = lastDirBackwards;
	    }
	    onRails = isRails;
	}
        noClip = onRails;
        this.getPassengers().forEach(entity -> entity.noClip = onRails);
        super.onUpdate();
        if(onRails && this.getControllingPassenger() != null) {
            minecart.onUpdate();
        }   
        prevOnRails = onRails;
    }
    
    @Override
    public void onEntityUpdate() {
//	System.out.println(Minecraft.getMinecraft().player.getLook(1f)); 1X = +1 ////////////// 0. 0. 1
        super.onEntityUpdate();
        if(onRails) {
            if(this.canPassengerSteer()) {
        	if (this.getPassengers().isEmpty() || !(this.getPassengers().get(0) instanceof EntityPlayer)) {
                    this.setControlState(0);
                }
                if(this.world.isRemote) {
            	this.handleControl(false); //+Z-X
                }
            }
        } else {
            rotationYawInterp.reset(this.rotationYaw - 180D);
        }
        lastDirBackwards = !forward() && backward();
    }
    
    @Override
    public float getSoundVolume() {
        return onRails ? this.getControllingPassenger() != null ? this.getSpeed().modifier / 2f : 0f : super.getSoundVolume();
    }
    
    @Override
    public EnumFacing getAdjustedHorizontalFacing() {
        return onRails ? minecart.getAdjustedHorizontalFacing() : super.getAdjustedHorizontalFacing();
    }

    @Override
    protected WheelData createWheels() {
	return new WheelData(1.3, 2, -1.3, -2.2);
    }
    
    @Override
    protected boolean shouldTyresRender() {
        return super.shouldTyresRender() && !onRails;
    }
    
    /* =================================== CAR END ===========================================*/
    /* ================================ MINECART START =======================================*/
    private static final DataParameter<Integer> ROLLING_AMPLITUDE = EntityDataManager.<Integer>createKey(EntityMinecart.class, DataSerializers.VARINT);
    private static final int[][][] MATRIX = new int[][][] {{{0, 0, -1}, {0, 0, 1}}, {{ -1, 0, 0}, {1, 0, 0}}, {{ -1, -1, 0}, {1, 0, 0}}, {{ -1, 0, 0}, {1, -1, 0}}, {{0, 0, -1}, {0, -1, 1}}, {{0, -1, -1}, {0, 0, 1}}, {{0, 0, 1}, {1, 0, 0}}, {{0, 0, 1}, { -1, 0, 0}}, {{0, 0, -1}, { -1, 0, 0}}, {{0, 0, -1}, {1, 0, 0}}};

    public class MinecartLogic {
	private boolean isInReverse;
	private boolean prevKeyDown;
	
	public EnumFacing getAdjustedHorizontalFacing() {
	    return this.isInReverse ? getHorizontalFacing().getOpposite().rotateY() : getHorizontalFacing().rotateY();
	}
	
	public int getRollingAmplitude() {
	    return FordExplorerEntity.this.dataManager.get(ROLLING_AMPLITUDE).intValue();
	}
	
	public void setRollingAmplitude(int rollingAmplitude) {
	    FordExplorerEntity.this.dataManager.set(ROLLING_AMPLITUDE, rollingAmplitude);
	}
	
	public void onUpdate() {
	    //CAR STUFF START
	    rotationDelta *= 0.8f;
	    allWheels.forEach(FordExplorerEntity.this::processWheel);
	    
	    List<WheelParticleData> markedRemoved = Lists.newArrayList();
	    wheelDataList.forEach(wheel -> wheel.onUpdate(markedRemoved));
	    markedRemoved.forEach(wheelDataList::remove);
	    //CAR STUFF END
	    
	    if (getRollingAmplitude() > 0) {
		setRollingAmplitude(getRollingAmplitude() - 1);
	    }

	    if (posY < -64.0D) {
		outOfWorld();
	    }

	    if (!world.isRemote && world instanceof WorldServer) {
		world.profiler.startSection("portal");
		MinecraftServer minecraftserver = world.getMinecraftServer();
		int i = getMaxInPortalTime();	
		if (inPortal) {
		    if (minecraftserver.getAllowNether()) {
			if (!isRiding() && portalCounter++ >= i) {
			    portalCounter = i;
			    timeUntilPortal = getPortalCooldown();
			    int j;
			    if (world.provider.getDimensionType().getId() == -1) {
				j = 0;
			    } else {
				j = -1;
			    }
			    
			    changeDimension(j);
			}

			inPortal = false;
		    }
		} else {
		    if (portalCounter > 0) {
			portalCounter -= 4;
		    }
		    
		    if (portalCounter < 0) {
			portalCounter = 0;
		    }
		}

		if (timeUntilPortal > 0) {
		    --timeUntilPortal;
		}

		world.profiler.endSection();
	    }
	    
	    prevPosX = posX;
	    prevPosY = posY;
	    prevPosZ = posZ;

	    if (!hasNoGravity()) {
		motionY -= 0.03999999910593033D;
	    }

	    int k = MathHelper.floor(posX);
	    int l = MathHelper.floor(posY);
	    int i1 = MathHelper.floor(posZ);
		
	    if (world.getBlockState(new BlockPos(k, l - 1, i1)).getBlock() instanceof TourRailBlock) {
		--l;
	    }
		
	    BlockPos blockpos = new BlockPos(k, l, i1);
	    IBlockState iblockstate = world.getBlockState(blockpos);
	    if(!(iblockstate.getBlock() instanceof TourRailBlock)) {
		return;
	    }
	    moveAlongTrack(blockpos, iblockstate);
		
	    doBlockCollisions();
	    rotationPitch = 0.0F;
		
	    AxisAlignedBB box = getEntityBoundingBox().grow(0.20000000298023224D, 0.0D, 0.20000000298023224D);

	    handleWaterMovement();
	}
	
	@SuppressWarnings("incomplete-switch")
	protected void moveAlongTrack(BlockPos pos, IBlockState state) {
	    if(!(state.getBlock() instanceof TourRailBlock)) {
		return;
	    }
	    fallDistance = 0.0F;
	    Vec3d vec3d = getPos(posX, posY, posZ);
	    posY = (double)pos.getY();

	    double slopeAdjustment = 0.0078125D;
	    BlockRailBase.EnumRailDirection blockrailbase$enumraildirection = state.getValue(((TourRailBlock)state.getBlock()).getShapeProperty());
	    
	    switch (blockrailbase$enumraildirection) {
	    case ASCENDING_EAST:
		motionX -= slopeAdjustment;
		++posY;
		break;
	    case ASCENDING_WEST:
		motionX += slopeAdjustment;
		++posY;
		break;
	    case ASCENDING_NORTH:
		motionZ += slopeAdjustment;
		++posY;
		break;
	    case ASCENDING_SOUTH:
		motionZ -= slopeAdjustment;
		++posY;
	    }
	    
	    int[][] aint = MATRIX[blockrailbase$enumraildirection.getMetadata()];
	    double d1 = (double)(aint[1][0] - aint[0][0]);
	    double d2 = (double)(aint[1][2] - aint[0][2]);
	    double d3 = Math.sqrt(d1 * d1 + d2 * d2);
	    double d4 = motionX * d1 + motionZ * d2;
	    
	    if (d4 < 0.0D) {
		d1 = -d1;
		d2 = -d2;
	    }

	    double d5 = Math.sqrt(motionX * motionX + motionZ * motionZ);
	    
	    if (d5 > 2.0D) {
		d5 = 2.0D;
	    }
	    double d = 1;
	    if(forward()) {
		if(!prevKeyDown && isInReverse) {
		    d = -1;
		}
		isInReverse = false;
		prevKeyDown = true;
	    } else if(backward()) {
		if(!prevKeyDown && !isInReverse) {
		    d = -1;
		}
		isInReverse = true;
		prevKeyDown = true;
	    } else {
		prevKeyDown = false;
	    }
	    
	    d5 *= d;
	    
	    motionX = d5 * d1 / d3;
	    motionZ = d5 * d2 / d3;
	    	    
	    double target = 0;
	    double d22;
	    
	    Vec3d vec = getPositionVector();
	    if(world.isRemote) {
		Vec3d dirVec = new Vec3d(-d1, 0, d2).add(vec);
		target = MathUtils.cosineFromPoints(vec.addVector(0, 0, 1), dirVec, vec);
		if(dirVec.x < vec.x) {
		    target = -target;
		}
		if(isInReverse) {
		    target += 180F;
		}
		
		do {
		    d22 = Math.abs(rotationYawInterp.getCurrent() - target);
		    double d23 = Math.abs(rotationYawInterp.getCurrent() - (target + 360f));
		    double d24 = Math.abs(rotationYawInterp.getCurrent() - (target - 360f));
			    
		    if(d23 < d22) {
			target += 360f;
		    } else if(d24 < d22) {
			target -= 360f;
		    }
		} while(d22 > 180);
		
		if(!prevOnRails) {
		    rotationYawInterp.reset(target);
		} else {
		    rotationYawInterp.setTarget(target);
		}
	    }
	    
	    setRotation((float) rotationYawInterp.getCurrent(), rotationPitch);
	    
	    Entity entity = getPassengers().isEmpty() ? null : (Entity)getPassengers().get(0);
	    
	    double d18 = (double)pos.getX() + 0.5D + (double)aint[0][0] * 0.5D;
	    double d19 = (double)pos.getZ() + 0.5D + (double)aint[0][2] * 0.5D;
	    double d20 = (double)pos.getX() + 0.5D + (double)aint[1][0] * 0.5D;
	    double d21 = (double)pos.getZ() + 0.5D + (double)aint[1][2] * 0.5D;
	    d1 = d20 - d18;
	    d2 = d21 - d19;
	    double d10;
	    
	    if (d1 == 0.0D) {
		posX = (double)pos.getX() + 0.5D;
		d10 = posZ - (double)pos.getZ();
	    } else if (d2 == 0.0D) {
		posZ = (double)pos.getZ() + 0.5D;
		d10 = posX - (double)pos.getX();
	    } else {
		double d11 = posX - d18;
		double d12 = posZ - d19;
		d10 = (d11 * d1 + d12 * d2) * 2.0D;
	    }
	    
	    posX = d18 + d1 * d10;
	    posZ = d19 + d2 * d10;
	    setPosition(posX, posY, posZ);
	    moveMinecartOnRail(pos);
	    
	    if (aint[0][1] != 0 && MathHelper.floor(posX) - pos.getX() == aint[0][0] && MathHelper.floor(posZ) - pos.getZ() == aint[0][2]) {
		setPosition(posX, posY + (double)aint[0][1], posZ);
	    } else if (aint[1][1] != 0 && MathHelper.floor(posX) - pos.getX() == aint[1][0] && MathHelper.floor(posZ) - pos.getZ() == aint[1][2])  {
		setPosition(posX, posY + (double)aint[1][1], posZ);
	    }
	    
	    double drag = isBeingRidden() ? 0.9D : 0.75D;
	    
	    motionX *= drag;
	    motionY *= 0.0D;
	    motionZ *= drag;
	    
	    Vec3d vec3d1 = getPos(posX, posY, posZ);
	    
	    if (vec3d1 != null && vec3d != null) {
		double d14 = (vec3d.y - vec3d1.y) * 0.05D;
		d5 = Math.sqrt(motionX * motionX + motionZ * motionZ);
		
		if (d5 > 0.0D) {
		    motionX = motionX / d5 * (d5 + d14);
		    motionZ = motionZ / d5 * (d5 + d14);
		}
		
		setPosition(posX, vec3d1.y, posZ);
	    }

	    int j = MathHelper.floor(posX);
	    int i = MathHelper.floor(posZ);
	    	    
	    if (j != pos.getX() || i != pos.getZ()) {
		d5 = Math.sqrt(motionX * motionX + motionZ * motionZ);
		motionX = d5 * (double)(j - pos.getX());
		motionZ = d5 * (double)(i - pos.getZ());
	    }
	    double d15 = Math.sqrt(motionX * motionX + motionZ * motionZ);
	    if(d15 == 0) {
		d15 = 1;
	    }
	    double d16 = 0.06D;
            motionX += motionX / d15 * d16;
            motionZ += motionZ / d15 * d16;
	}
	
	public Vec3d getPos(double p_70489_1_, double p_70489_3_, double p_70489_5_) {
	    int i = MathHelper.floor(p_70489_1_);
	    int j = MathHelper.floor(p_70489_3_);
	    int k = MathHelper.floor(p_70489_5_);
	    
	    if (world.getBlockState(new BlockPos(i, j - 1, k)).getBlock() instanceof TourRailBlock) {
		--j;
	    }

	    IBlockState iblockstate = world.getBlockState(new BlockPos(i, j, k));
	    
	    if (iblockstate.getBlock() instanceof TourRailBlock)
	    {
		BlockRailBase.EnumRailDirection blockrailbase$enumraildirection = iblockstate.getValue(((TourRailBlock) iblockstate.getBlock()).getShapeProperty());
		int[][] aint = MATRIX[blockrailbase$enumraildirection.getMetadata()];
		double d0 = (double)i + 0.5D + (double)aint[0][0] * 0.5D;
		double d1 = (double)j + 0.0625D + (double)aint[0][1] * 0.5D;
		double d2 = (double)k + 0.5D + (double)aint[0][2] * 0.5D;
		double d3 = (double)i + 0.5D + (double)aint[1][0] * 0.5D;
		double d4 = (double)j + 0.0625D + (double)aint[1][1] * 0.5D;
		double d5 = (double)k + 0.5D + (double)aint[1][2] * 0.5D;
		double d6 = d3 - d0;
		double d7 = (d4 - d1) * 2.0D;
		double d8 = d5 - d2;
		double d9;
		
		if (d6 == 0.0D) {
		    d9 = p_70489_5_ - (double)k;
		} else if (d8 == 0.0D) {
		    d9 = p_70489_1_ - (double)i;
		} else {
		    double d10 = p_70489_1_ - d0;
		    double d11 = p_70489_5_ - d2;
		    d9 = (d10 * d6 + d11 * d8) * 2.0D;
		}

		p_70489_1_ = d0 + d6 * d9;
		p_70489_3_ = d1 + d7 * d9;
		p_70489_5_ = d2 + d8 * d9;
		
		if (d7 < 0.0D) {
		    ++p_70489_3_;
		}

		if (d7 > 0.0D) {
		    p_70489_3_ += 0.5D;
		}

		return new Vec3d(p_70489_1_, p_70489_3_, p_70489_5_);
	    }
	    else {
		return null;
	    }
	}
	
	public void moveMinecartOnRail(BlockPos pos) {
	    double mX = motionX;
	    double mZ = motionZ;
	    if(mX == 0 && mZ == 0 && getControllingPassenger() != null) { //Should only happen when re-logging. //TODO: make a more elegant solution
		mX = getLook(1f).x;
		mZ = getLook(1f).z;
	    }

	    double max = this.getMaxSpeed();
	    mX = MathHelper.clamp(mX, -max, max);
	    mZ = MathHelper.clamp(mZ, -max, max);
	    FordExplorerEntity.this.move(MoverType.SELF, mX, 0.0D, mZ);
	}
	
	protected double getMaxSpeed() {
	    return getSpeed().modifier / 4f;
	}
	
	@Nullable
	@SideOnly(Side.CLIENT)
	public Vec3d getPosOffset(double x, double y, double z, double offset)
	{
	    int i = MathHelper.floor(x);
	    int j = MathHelper.floor(y);
	    int k = MathHelper.floor(z);

	    if (world.getBlockState(new BlockPos(i, j - 1, k)).getBlock() instanceof TourRailBlock)
	    {
		--j;
	    }

	    IBlockState iblockstate = world.getBlockState(new BlockPos(i, j, k));
	    
	    if (iblockstate.getBlock() instanceof TourRailBlock) {
		BlockRailBase.EnumRailDirection blockrailbase$enumraildirection = iblockstate.getValue(((TourRailBlock) iblockstate.getBlock()).getShapeProperty());
		y = (double)j;
		
		if (blockrailbase$enumraildirection.isAscending())
		{
		    y = (double)(j + 1);
		}

		int[][] aint = MATRIX[blockrailbase$enumraildirection.getMetadata()];
		double d0 = (double)(aint[1][0] - aint[0][0]);
		double d1 = (double)(aint[1][2] - aint[0][2]);
		double d2 = Math.sqrt(d0 * d0 + d1 * d1);
		d0 = d0 / d2;
		d1 = d1 / d2;
		x = x + d0 * offset;
		z = z + d1 * offset;
		
		if (aint[0][1] != 0 && MathHelper.floor(x) - i == aint[0][0] && MathHelper.floor(z) - k == aint[0][2])
		{
		    y += (double)aint[0][1];
		}
		else if (aint[1][1] != 0 && MathHelper.floor(x) - i == aint[1][0] && MathHelper.floor(z) - k == aint[1][2])
		{
		    y += (double)aint[1][1];
		}
		
		return this.getPos(x, y, z);
	    } else {
		return null;
	    }
	}
    }
    
    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(ROLLING_AMPLITUDE, Integer.valueOf(0));
    }
    
    
    /* ================================= MINECART END ========================================*/


}
