package com.sarinsa.morenodestuff.common.core.registry;

import com.sarinsa.morenodestuff.common.tileentity.NodeAgitatorTileEntity;
import cpw.mods.fml.common.registry.GameRegistry;

public class MNSTileEntities {
    
    public static void register() {
        GameRegistry.registerTileEntity( NodeAgitatorTileEntity.class, "node_agitator" );
    }
}
