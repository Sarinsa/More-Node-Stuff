package com.sarinsa.morenodestuff.common.block;

import com.sarinsa.morenodestuff.common.Util;
import com.sarinsa.morenodestuff.common.tileentity.NodeAgitatorTileEntity;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
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
        return Util.BlockRenderID.NODE_AGITATOR;
    }
}
