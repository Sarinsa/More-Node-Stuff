package com.sarinsa.morenodestuff.client.render.block;

import com.sarinsa.morenodestuff.common.tileentity.NodeAgitatorTileEntity;
import com.sarinsa.morenodestuff.common.util.MNSRenderTypes;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

public class NodeAgitatorBlockRenderer implements ISimpleBlockRenderingHandler {
    
    @Override
    public void renderInventoryBlock( Block block, int metadata, int modelID, RenderBlocks renderer ) {
        GL11.glTranslatef( -0.5F, -0.5F, -0.5F );
        TileEntityRendererDispatcher.instance.renderTileEntityAt(
                new NodeAgitatorTileEntity(),
                0.0F, 0.0F, 0.0F, 0.0F
        );
    }
    
    @Override
    public boolean renderWorldBlock( IBlockAccess world, int x, int y, int z, Block block, int id, RenderBlocks renderer ) {
        renderer.clearOverrideBlockTexture();
        block.setBlockBounds( 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F );
        renderer.setRenderBoundsFromBlock( block );
        return true;
    }
    
    @Override
    public boolean shouldRender3DInInventory( int id ) {
        return true;
    }
    
    @Override
    public int getRenderId() {
        return MNSRenderTypes.NODE_AGITATOR;
    }
}
