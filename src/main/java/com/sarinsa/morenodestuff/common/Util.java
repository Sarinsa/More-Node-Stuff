package com.sarinsa.morenodestuff.common;

public class Util {
    
    public static class BlockRenderTypes {
        
        // Skips normal rendering
        public static final int TILE_ENTITY = -1;
        // Normal full cube block
        public static final int DEFAULT = 0;
        // Flowers, tall grass, crops
        public static final int CROSS = 1;
        // Torches
        public static final int TORCH = 2;
        // Fire block
        public static final int FIRE = 3;
    }
    
    /** References for block render IDs. Populated on the client, defaults to -1 on the server. */
    public static class BlockRenderID {
        
        public static int NODE_AGITATOR = -1;
    }
}
