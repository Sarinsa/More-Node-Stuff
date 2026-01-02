package com.sarinsa.morenodestuff.client;

import com.sarinsa.morenodestuff.client.render.block.NodeAgitatorBlockRenderer;
import com.sarinsa.morenodestuff.client.render.tile.NodeAgitatorTileRenderer;
import com.sarinsa.morenodestuff.common.core.CommonProxy;
import com.sarinsa.morenodestuff.common.tileentity.NodeAgitatorTileEntity;
import com.sarinsa.morenodestuff.common.util.MNSRenderTypes;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class ClientProxy extends CommonProxy {
    
    @Override
    public void clientSetup() {
        registerTileRenderers();
        registerBlockRenderers();
    }
    
    private void registerTileRenderers() {
        ClientRegistry.bindTileEntitySpecialRenderer( NodeAgitatorTileEntity.class, new NodeAgitatorTileRenderer() );
    }
    
    private void registerBlockRenderers() {
        MNSRenderTypes.NODE_AGITATOR = RenderingRegistry.getNextAvailableRenderId();
        RenderingRegistry.registerBlockHandler( new NodeAgitatorBlockRenderer() );
    }
}
