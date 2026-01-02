package com.sarinsa.morenodestuff.common.tileentity;

import com.sarinsa.morenodestuff.common.core.registry.MNSBlocks;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
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
import thaumcraft.api.nodes.INode;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.fx.PacketFXBlockZap;
import thaumcraft.common.tiles.TileNode;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Quick summary of what this thing is supposed to do:
 * <br><br>
 * The <b>Node Agitator</b> is a "sidegrade" of the <b>Node Stabilizer</b>.
 * Instead of keeping a node safe and cozy it promotes accelerated node bullying.
 * <br><br>
 * When active, the node above the agitator will attempt to steal aspects and capacity from nearby nodes
 * at roughly twice the normal speed.
 * <br><br>
 * If the agitator is given {@link Aspect#HUNGER} essentia, the multiplier is bumped up from x2 to x3.
 * This is not without <span style="color: red">risk</span> however, and the node has a tiny chance every tick to turn into a hungry node.
 */
public class NodeAgitatorTileEntity extends TileThaumcraft implements IAspectContainer, IEssentiaTransport {
    
    /** The aspect of the currently held essentia. Should only ever be {@link Aspect#HUNGER} or null. */
    private Aspect aspect = null;
    /** The amount of essentia currently held by this node agitator. */
    private int amount = 0;
    /** The maximum amount of essentia the node agitator can hold. */
    private static final int maxAmount = 64;
    
    /** The block face where essentia can be inputted. */
    private static final ForgeDirection inputFacing = ForgeDirection.DOWN;
    /** Convenience reference to the aspect the node agitator accepts. */
    private static final Aspect acceptedAspect = Aspect.HUNGER;
    
    /** The delay in ticks before the node agitator can draw essentia again from any transporter. */
    private int drawEssDelay = 0;
    
    /**
     * Used in {@link com.sarinsa.morenodestuff.client.render.tile.NodeAgitatorTileRenderer#renderTileEntityAt(NodeAgitatorTileEntity, double, double, double, float)}
     * when rendering the pistons that extend from the base model.
     */
    public int pistonExt = 0;
    
    public NodeAgitatorTileEntity() { }
    
    /**
     * Called on both sides when this tile entity is getting ticked.
     */
    @Override
    public void updateEntity() {
        super.updateEntity();
        
        // Try drawing essentia from any connected transporters
        if( !worldObj.isRemote ) {
            drawEssentia();
        }
        final Block block = worldObj.getBlock( xCoord, yCoord, zCoord );
        
        // If for whatever reason the block at our position
        // is not an agitator block we abort.
        if( block != MNSBlocks.NODE_AGITATOR ) return;
        
        agitateNode();
    }
    
    /** Handles the main logic for the Node Agitator (messing with the node above it). */
    private void agitateNode() {
        if( yCoord < worldObj.getHeight() - 1 ) {
            int aboveMeta = worldObj.getBlockMetadata( xCoord, yCoord + 1, zCoord );
            TileEntity nodeTile = worldObj.getTileEntity( xCoord, yCoord + 1, zCoord );
            
            if( nodeTile instanceof INode && (aboveMeta == 0 || aboveMeta == 5) && !worldObj.isBlockIndirectlyGettingPowered( xCoord, yCoord, zCoord ) ) {
                if( !worldObj.isRemote ) {
                    INode node = (INode) nodeTile;
                    NodeModifier modifier = node.getNodeModifier();
                    NodeType type = node.getNodeType();
                    double bullyChance = 0.03;
                    double incChanceMult = 0.0;
                    
                    if( getCurrentAspect() == acceptedAspect && getEssentiaAmount( null ) > 0 ) {
                        bullyChance += 0.3;
                        incChanceMult += 0.1;
                        
                        // Drain internal essentia once every second. This is expensive!
                        if( worldObj.getTotalWorldTime() % 20 == 0 ) {
                            --amount;
                            worldObj.markBlockForUpdate( xCoord, yCoord, zCoord );
                            nodeTile.markDirty();
                        }
                        // Tiny chance to change the node into a hungry node #Prank
                        if( type != NodeType.HUNGRY && worldObj.rand.nextInt( 10000 ) == 0 ) {
                            node.setNodeType( NodeType.HUNGRY );
                            worldObj.markBlockForUpdate( xCoord, yCoord + 1, zCoord );
                            nodeTile.markDirty();
                        }
                    }
                    // Apply chance modifier based on node type
                    boolean hungryOrBright = type == NodeType.HUNGRY || modifier == NodeModifier.BRIGHT;
                    bullyChance += modifier == null ? 0 : (hungryOrBright ? 0.02 : (modifier == NodeModifier.PALE ? -0.02 : 0));
                    
                    if( worldObj.rand.nextDouble() <= bullyChance ) {
                        // Try bullying.
                        if( tryBullyNearby( (TileNode) node, incChanceMult ) ) {
                            // Update bully node if it changed.
                            ((TileNode) node).markDirty();
                            worldObj.markBlockForUpdate( xCoord, yCoord + 1, zCoord );
                        }
                    }
                }
                // Extend pistons
                if( pistonExt < 37 )
                    ++pistonExt;
            }
            // Retract pistons
            else if( pistonExt > 0 )
                --pistonExt;
        }
    }
    
    /**
     * A modified copy-paste of {@link TileNode#handleDischarge(boolean)}.
     * <br><br>
     * Looks for nearby nodes within a 5x5 area of the bullying node
     * to try and steal aspects from.
     *
     * @param bullyNode     The node that is trying to steal aspects from nearby nodes.
     * @param incChanceMult Additional multiplier for increasing/reducing odds of
     *                      the bully node increasing its max capacity when zapping the target.
     * @return True if the bully node needs to sync data changes.
     */
    @SuppressWarnings( "JavadocReference" )
    private boolean tryBullyNearby( TileNode bullyNode, double incChanceMult ) {
        if( worldObj.getBlock( bullyNode.xCoord, bullyNode.yCoord, bullyNode.zCoord ) != ConfigBlocks.blockAiry || bullyNode.getLock() == 1 )
            return false;
        
        NodeModifier modifier = bullyNode.getNodeModifier();
        NodeType type = bullyNode.getNodeType();
        
        // Fading nodes can not bully other nodes.
        if( modifier == NodeModifier.FADING )
            return false;
        
        // Pale nodes have a 50% chance to not bully, they are weak.
        if( modifier == NodeModifier.PALE && worldObj.rand.nextBoolean() )
            return false;
        
        final List<TileNode> nearbyNodes = new ArrayList<>();
        
        // Scan for nearby nodes.
        for( int x = -4; x < 4; x++ ) {
            for( int y = -4; y < 4; y++ ) {
                for( int z = -4; z < 4; z++ ) {
                    TileEntity te = worldObj.getTileEntity( bullyNode.xCoord + x, bullyNode.yCoord + y, bullyNode.zCoord + z );
                    
                    if( te instanceof TileNode )
                        nearbyNodes.add( (TileNode) te );
                }
            }
        }
        // Make sure we don't pick the bully node as target node.
        nearbyNodes.remove( bullyNode );
        
        // Return is there are no nearby nodes.
        if( nearbyNodes.isEmpty() ) return false;
        
        // Pick a random node from the list of nearby nodes.
        final TileNode nodeToZap = nearbyNodes.get( worldObj.rand.nextInt( nearbyNodes.size() ) );
        final int x = nodeToZap.xCoord;
        final int y = nodeToZap.yCoord;
        final int z = nodeToZap.zCoord;
        
        // Check if the block at the picked location is a node block, just in case.
        if( worldObj.getBlock( x, y, z ) != ConfigBlocks.blockAiry )
            return false;
        
        // Check if the node to bully is "locked".
        // getLock() > 0 usually indicates the node is being
        // protected by a node stabilizer.
        if( nodeToZap.getLock() > 0 )
            return false;
        
        int toZapAvg = (nodeToZap.getAspects().visSize() + nodeToZap.getAspectsBase().visSize()) / 2;
        int bullyAvg = (bullyNode.getAspects().visSize() + bullyNode.getAspectsBase().visSize()) / 2;
        
        // Check if the target node is smaller than the bully.
        // Also don't try zapping nodes that have lost all their aspects.
        if( toZapAvg > bullyAvg || nodeToZap.getAspects().size() == 0 )
            return false;
        
        // Pick a random aspect of the target to try and steal vis from.
        Aspect aspect = nodeToZap.getAspects().getAspects()[worldObj.rand.nextInt( nodeToZap.getAspects().size() )];
        boolean zapAndUpdate = false;
        
        // If the bully is not fully replenished in the chosen aspect,
        // we just "recharge" the bully node by one point instead of
        // increasing the maximum vis for that aspect.
        if( bullyNode.getAspects().getAmount( aspect ) < bullyNode.getNodeVisBase( aspect ) && nodeToZap.takeFromContainer( aspect, 1 ) ) {
            bullyNode.addToContainer( aspect, 1 );
            zapAndUpdate = true;
        }
        else if( nodeToZap.takeFromContainer( aspect, 1 ) ) {
            int sizeOfAspect = bullyNode.getNodeVisBase( aspect );
            double incMaxChance = Math.min( 1.0 / sizeOfAspect + incChanceMult, 1.0 );
            
            if( worldObj.rand.nextDouble() <= incMaxChance ) {
                bullyNode.getAspectsBase().add( aspect, 1 );
                
                // Small chance for the bully node to turn normal if it is pale
                if( bullyNode.getNodeModifier() == NodeModifier.PALE && worldObj.rand.nextInt( 200 ) == 0 ) {
                    bullyNode.setNodeModifier( null );
                    // TODO Maybe use mixin to access these fields without dumb reflection
                    //bullyNode.regeneration = -1;
                }
                // A 1/6 chance of the zapped node losing the aspect point
                // that was stolen, permanently.
                if( worldObj.rand.nextInt( 6 ) == 0 ) {
                    nodeToZap.setNodeVisBase( aspect, (short) (nodeToZap.getNodeVisBase( aspect ) - 1) );
                }
            }
            zapAndUpdate = true;
        }
        
        if( zapAndUpdate ) {
            // TODO Maybe use mixin to access these fields without dumb reflection
            //((TileNode) tileEntity).wait = ((TileNode) tileEntity).regeneration / 2;
            worldObj.markBlockForUpdate( x, y, z );
            nodeToZap.markDirty();
            
            // Send zap visual effect to client of nearby players
            PacketHandler.INSTANCE.sendToAllAround( new PacketFXBlockZap(
                            (float) x + 0.5F,
                            (float) y + 0.5F,
                            (float) z + 0.5F,
                            (float) bullyNode.xCoord + 0.5F,
                            (float) bullyNode.yCoord + 0.5F,
                            (float) bullyNode.zCoord + 0.5F
                    ),
                    new NetworkRegistry.TargetPoint(
                            worldObj.provider.dimensionId,
                            x,
                            y,
                            z,
                            32.0F
                    )
            );
        }
        return zapAndUpdate;
    }
    
    /** Attempts to draw essentia from a connected essentia transporter. */
    private void drawEssentia() {
        // 5-tick delay between each attempt
        if( ++drawEssDelay == 5 ) {
            drawEssDelay = 0;
            TileEntity te = ThaumcraftApiHelper.getConnectableTile( worldObj, xCoord, yCoord, zCoord, inputFacing );
            
            // No transporter found
            if( te == null ) return;
            
            IEssentiaTransport essentiaTransport = (IEssentiaTransport) te;
            
            // Can the transporter input essentia?
            if( !essentiaTransport.canOutputTo( inputFacing.getOpposite() ) ) {
                return;
            }
            
            // Check suction difference
            if( essentiaTransport.getSuctionAmount( inputFacing.getOpposite() ) < getSuctionAmount( inputFacing ) ) {
                // Try grabbing essentia from the transporter
                // and adding it to self.
                int takenEssentia = essentiaTransport.takeEssentia( acceptedAspect, 1, inputFacing.getOpposite() );
                addEssentia( acceptedAspect, takenEssentia, inputFacing );
            }
        }
    }
    
    /**
     * @return The aspect of the essentia
     * currently held by this node agitator.
     * Should only ever be {@link Aspect#HUNGER} or null
     * if {@link NodeAgitatorTileEntity#amount} is 0.
     */
    @Nullable
    public Aspect getCurrentAspect() {
        return aspect;
    }
    
    @Override // TileThaumcraft
    public void readCustomNBT( NBTTagCompound tagCompound ) {
        aspect = Aspect.getAspect( tagCompound.getString( "Aspect" ) );
        amount = tagCompound.getShort( "Amount" );
    }
    
    @Override // TileThaumcraft
    public void writeCustomNBT( NBTTagCompound tagCompound ) {
        if( aspect != null ) {
            tagCompound.setString( "Aspect", aspect.getTag() );
        }
        tagCompound.setShort( "Amount", (short) amount );
    }
    
    //
    //                      IEssentiaTransport
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
    //                       IAspectContainer
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
