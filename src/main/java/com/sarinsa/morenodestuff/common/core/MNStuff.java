package com.sarinsa.morenodestuff.common.core;

import com.sarinsa.morenodestuff.common.core.registry.MNSBlocks;
import com.sarinsa.morenodestuff.common.core.registry.MNSItems;
import com.sarinsa.morenodestuff.common.core.registry.MNSTileEntities;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod( modid = MNStuff.MODID, useMetadata = true )
public class MNStuff {
    
    public static final String MODID = "morenodestuff";
    public static final Logger LOG = LogManager.getLogger( MODID );
    
    @SidedProxy(
            modId = MNStuff.MODID,
            clientSide = "com.sarinsa.morenodestuff.client.ClientProxy",
            serverSide = "com.sarinsa.morenodestuff.common.core.CommonProxy"
    )
    public static CommonProxy proxy;
    
    
    @EventHandler
    public void init( FMLInitializationEvent event ) {
        MNSBlocks.register();
        MNSItems.register();
        MNSTileEntities.register();
        
        proxy.clientSetup();
        proxy.serverSetup();
    }
}
