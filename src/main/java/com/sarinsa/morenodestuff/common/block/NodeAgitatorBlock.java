package com.sarinsa.morenodestuff.common.block;

import com.sarinsa.morenodestuff.common.tileentity.NodeAgitatorTileEntity;
import com.sarinsa.morenodestuff.common.util.MNSRenderTypes;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class NodeAgitatorBlock extends BlockContainer {
    
    public NodeAgitatorBlock( Material material ) {
        super( material );
    }
    
    @Override
    public TileEntity createNewTileEntity( World world, int metadata ) {
        return new NodeAgitatorTileEntity();
    }
    
    @Override
    public boolean isOpaqueCube() {
        return false;
    }
    
    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }
    
    @Override
    public int getRenderType() {
        return MNSRenderTypes.NODE_AGITATOR;
    }
    
    @SideOnly( Side.CLIENT )
    @Override
    public void registerBlockIcons( IIconRegister iconRegister ) {
        blockIcon = iconRegister.registerIcon( new ResourceLocation( "thaumcraft", "arcane_stone" ).toString() );
    }
}
