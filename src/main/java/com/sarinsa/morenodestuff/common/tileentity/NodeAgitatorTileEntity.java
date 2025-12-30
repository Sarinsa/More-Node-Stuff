package com.sarinsa.morenodestuff.common.tileentity;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.TileThaumcraft;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;

public class NodeAgitatorTileEntity extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {
    
    /** The aspect of the currently held essentia. Should only ever be {@link Aspect#GREED} or null. */
    private Aspect aspect = null;
    /** The amount of essentia currently held by this node agitator. */
    private int amount = 0;
    /** The maximum amount of essentia the node agitator can hold. */
    private static final int maxAmount = 64;
    private static final ForgeDirection inputFacing = ForgeDirection.DOWN;
    private static final Aspect acceptedAspect = Aspect.HUNGER;
    /** The delay in ticks before the node agitator can draw essentia again from any transporter. */
    private int drawEssDelay = 0;
    
    public int count = 0;
    public int lock = 0;
    
    
    public NodeAgitatorTileEntity() { }
    
    /**
     * Called on both sides when this tile entity is getting ticked.
     */
    @Override
    public void updateEntity() {
        if( !worldObj.isRemote ) {
            drawEssentia();
        }
    }
    
    /** Attempts to draw essentia from a connected essentia transporter. */
    private void drawEssentia() {
        if( ++drawEssDelay == 5 ) {
            drawEssDelay = 0;
            TileEntity te = ThaumcraftApiHelper.getConnectableTile( worldObj, xCoord, yCoord, zCoord, inputFacing );
            
            if( te != null ) {
                IEssentiaTransport essentiaTransport = (IEssentiaTransport) te;
                
                if( !essentiaTransport.canOutputTo( inputFacing.getOpposite() ) ) {
                    return;
                }
                
                if( essentiaTransport.getSuctionAmount( inputFacing.getOpposite() ) < getSuctionAmount( inputFacing )
                        && essentiaTransport.takeEssentia( acceptedAspect, 1, inputFacing.getOpposite() ) == 1 ) {
                    addEssentia( acceptedAspect, 1, inputFacing );
                }
            }
        }
    }
    
    /** @return The amount of essentia currently held by this node agitator. */
    public int getEssentiaAmount() {
        return amount;
    }
    
    /**
     * @return The aspect of the essentia
     * currently held by this node agitator.
     * Should only ever be {@link Aspect#HUNGER} or null
     * if {@link NodeAgitatorTileEntity#amount} is 0.
     */
    public Aspect getCurrentAspect() {
        return aspect;
    }
    
    @Override
    public void readCustomNBT( NBTTagCompound tagCompound ) {
        aspect = Aspect.getAspect( tagCompound.getString( "Aspect" ) );
        amount = tagCompound.getShort( "Amount" );
    }
    
    @Override
    public void writeCustomNBT( NBTTagCompound tagCompound ) {
        if( aspect != null ) {
            tagCompound.setString( "Aspect", aspect.getTag() );
        }
        tagCompound.setShort( "Amount", (short) amount );
    }
    
    //
    // IEssentiaTransport
    //
    
    /**
     * Is this tile able to connect to other vis users/sources on the specified side?
     */
    @Override
    public boolean isConnectable( ForgeDirection face ) {
        return face == inputFacing;
    }
    
    /**
     * Is this side used to input essentia?
     */
    @Override
    public boolean canInputFrom( ForgeDirection face ) {
        return face == inputFacing;
    }
    
    /**
     * Is this side used to output essentia?
     */
    @Override
    public boolean canOutputTo( ForgeDirection face ) {
        // Can't output
        return false;
    }
    
    /**
     * Sets the amount of suction this block will apply
     */
    @Override
    public void setSuction( Aspect aspect, int amount ) {
    
    }
    
    /**
     * Returns the type of suction this block is applying.
     *
     * @param face the location from where the suction is being checked
     * @return a return type of null indicates the suction is untyped and the first thing available will be drawn
     */
    @Override
    public Aspect getSuctionType( ForgeDirection face ) {
        return acceptedAspect;
    }
    
    /**
     * Returns the strength of suction this block is applying.
     *
     * @param face the location from where the suction is being checked
     */
    @Override
    public int getSuctionAmount( ForgeDirection face ) {
        return amount < maxAmount ? 64 : 0;
    }
    
    /**
     * remove the specified amount of essentia from this transport tile
     *
     * @return how much was actually taken
     */
    @Override
    public int takeEssentia( Aspect aspect, int amount, ForgeDirection face ) {
        // Can't take from
        return 0;
    }
    
    /**
     * add the specified amount of essentia to this transport tile
     *
     * @return how much was actually added
     */
    @Override
    public int addEssentia( Aspect aspect, int amt, ForgeDirection face ) {
        return canInputFrom( face )
                ? amt - addToContainer( aspect, amt )
                : 0;
    }
    
    /**
     * What type of essentia this contains
     */
    @Override
    public Aspect getEssentiaType( ForgeDirection face ) {
        return aspect;
    }
    
    /**
     * How much essentia this block contains
     */
    @Override
    public int getEssentiaAmount( ForgeDirection face ) {
        return amount;
    }
    
    /**
     * Essentia will not be drawn from this container unless the suction exceeds this amount.
     *
     * @return the amount
     */
    @Override
    public int getMinimumSuction() {
        return 128;
    }
    
    /**
     * Return true if you want the conduit to extend a little further into the block.
     * Used by jars and alembics that have smaller than normal hitboxes
     */
    @Override
    public boolean renderExtendedTube() {
        return false;
    }
    
    
    //
    // IAspectContainer
    //
    
    /** @return The aspects contained in this tile entity. */
    @Override
    public AspectList getAspects() {
        AspectList aspectList = new AspectList();
        
        if( aspect != null && amount > 0 )
            aspectList.add( aspect, amount );
        
        return aspectList;
    }
    
    @Override
    public void setAspects( AspectList aspectList ) {
        // NOOP
    }
    
    /**
     * This method is used to determine of a specific aspect can be added to this container.
     *
     * @return true or false
     */
    @Override
    public boolean doesContainerAccept( Aspect aspect ) {
        return aspect == acceptedAspect;
    }
    
    /**
     * This method is used to add a certain amount of an aspect to the tile entity.
     *
     * @return the amount of aspect left over that could not be added.
     */
    @Override
    public int addToContainer( Aspect aspct, int amt ) {
        if( amt > 0 ) {
            if( amount < maxAmount && aspct == aspect || amount == 0 ) {
                aspect = aspct;
                int added = Math.min( amt, maxAmount - amount );
                amount += added;
                amt -= added;
            }
            
            worldObj.markBlockForUpdate( xCoord, yCoord, zCoord );
            markDirty();
        }
        return amt;
    }
    
    /**
     * Removes a certain amount of a specific aspect from the tile entity
     *
     * @return true if that amount of aspect was available and was removed
     */
    @Override
    public boolean takeFromContainer( Aspect aspect, int amount ) {
        // Don't allow taking.
        return false;
    }
    
    /**
     * removes a bunch of different aspects and amounts from the tile entity.
     *
     * @param aspectList the ObjectTags object that contains the aspects and their amounts.
     * @return true if all the aspects and their amounts were available and successfully removed
     */
    @Deprecated
    @Override
    public boolean takeFromContainer( AspectList aspectList ) {
        // Don't allow taking.
        return false;
    }
    
    /**
     * @return True if the tile entity contains the listed amount (or more) of the aspect.
     */
    @Override
    public boolean doesContainerContainAmount( Aspect aspct, int amt ) {
        return aspct == aspect && amt <= amount;
    }
    
    /**
     * @param aspectList the AspectList that contains the aspects and their amounts.
     * @return True if the tile entity contains all the listed aspects and their amounts
     */
    @Deprecated
    @Override
    public boolean doesContainerContain( AspectList aspectList ) {
        for( Aspect aspct : aspectList.getAspects() ) {
            if( amount > 0 && aspct == aspect ) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns how much of the aspect this tile entity contains
     *
     * @return the amount of that aspect found
     */
    @Override
    public int containerContains( Aspect aspect ) {
        return 0;
    }
    
    /** @return The bounding box used by the tile entity renderer to check if it should render. */
    @SideOnly( Side.CLIENT )
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return AxisAlignedBB.getBoundingBox(
                xCoord, yCoord, zCoord,
                xCoord + 1, yCoord + 2, zCoord + 1
        );
    }
}
