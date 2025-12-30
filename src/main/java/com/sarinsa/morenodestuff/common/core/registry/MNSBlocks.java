package com.sarinsa.morenodestuff.common.core.registry;

import com.sarinsa.morenodestuff.common.block.NodeAgitatorBlock;
import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class MNSBlocks {
    
    public static Block NODE_AGITATOR;
    
    public static void register() {
        NODE_AGITATOR = register( new NodeAgitatorBlock( Material.rock ), "node_agitator" );
    }
    
    /**
     * Registers the given block with the specified registry name.
     * <br><br>
     * Assigns the registry name as unlocalized name and texture name.
     */
    private static Block register( Block block, String name ) {
        return GameRegistry.registerBlock( block.setBlockName( name ).setBlockTextureName( name ), name );
    }
}
