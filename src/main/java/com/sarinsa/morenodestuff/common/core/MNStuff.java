package com.sarinsa.morenodestuff.common.core;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.init.Blocks;

@Mod( modid = MNStuff.MODID, useMetadata = true )
public class MNStuff {
    
    public static final String MODID = "morenodestuff";
    
    @EventHandler
    public void init( FMLInitializationEvent event ) {
        // some example code
        System.out.println( "DIRT BLOCK >> " + Blocks.dirt.getUnlocalizedName() );
    }
}
