package com.sarinsa.morenodestuff.client.render.tile;

import com.sarinsa.morenodestuff.common.core.MNStuff;
import com.sarinsa.morenodestuff.common.tileentity.NodeAgitatorTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.AdvancedModelLoader;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;
import thaumcraft.client.lib.UtilsFX;

/**
 * Modified copy-paste of {@link thaumcraft.client.renderers.tile.TileNodeStabilizerRenderer}.
 * <br><br>
 * All credit goes to Azanor.
 */
public class NodeAgitatorTileRenderer extends TileEntitySpecialRenderer {
    
    private IModelCustom model;
    
    private static final ResourceLocation MODEL = new ResourceLocation( "thaumcraft", "textures/models/node_stabilizer.obj" );
    private static final ResourceLocation BASE_TEXTURE = new ResourceLocation( MNStuff.MODID, "textures/models/node_agitator.png" );
    private static final ResourceLocation OVERLAY_TEXTURE = new ResourceLocation( MNStuff.MODID, "textures/models/node_agitator_overlay.png" );
    private static final ResourceLocation BUBBLE_TEXTURE = new ResourceLocation( "thaumcraft", "textures/misc/node_bubble.png" );
    
    
    public NodeAgitatorTileRenderer() {
        model = AdvancedModelLoader.loadModel( MODEL );
    }
    
    public void renderTileEntityAt( NodeAgitatorTileEntity tile, double x, double y, double z, float partialTick ) {
        int lock = 1;
        int bright = 20;
        World world = tile.getWorldObj();
        
        if( world != null ) {
            if( world.getBlockMetadata( tile.xCoord, tile.yCoord, tile.zCoord ) == 10 ) {
                lock = 2;
            }
            bright = tile.getBlockType().getMixedBrightnessForBlock( world, tile.xCoord, tile.yCoord, tile.zCoord );
        }
        else {
            lock = tile.lock;
        }
        
        GL11.glPushMatrix();
        GL11.glTranslatef( (float) x + 0.5F, (float) y, (float) z + 0.5F );
        GL11.glRotatef( 90.0F, -1.0F, 0.0F, 0.0F );
        GL11.glColor4f( 1.0F, 1.0F, 1.0F, 1.0F );
        UtilsFX.bindTexture( BASE_TEXTURE );
        model.renderPart( "lock" );
        
        for( int a = 0; a < 4; ++a ) {
            GL11.glPushMatrix();
            if( world != null ) {
                int brightX = bright % 65536;
                int brightY = bright / 65536;
                OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, (float) brightX, (float) brightY );
            }
            
            GL11.glRotatef( (float) (90 * a), 0.0F, 0.0F, 1.0F );
            GL11.glRotatef( 45.0F, 0.0F, 1.0F, 0.0F );
            GL11.glTranslatef( 0.0F, 0.0F, (float) tile.count / 100.0F );
            UtilsFX.bindTexture( BASE_TEXTURE );
            model.renderPart( "piston" );
            
            if( lock == 2 ) {
                GL11.glColor4f( 1.0F, 0.2F, 0.2F, 1.0F );
            }
            
            if( world != null ) {
                float scale = MathHelper.sin( (float) (Minecraft.getMinecraft().renderViewEntity.ticksExisted + a * 5) / 3.0F ) * 0.1F + 0.9F;
                int brightness = 50 + (int) (170.0F * (float) tile.count / 37.0F * scale);
                int brightX = brightness % 65536;
                int brightY = brightness / 65536;
                OpenGlHelper.setLightmapTextureCoords( OpenGlHelper.lightmapTexUnit, (float) brightX, (float) brightY );
            }
            
            UtilsFX.bindTexture( OVERLAY_TEXTURE );
            model.renderPart( "piston" );
            GL11.glColor4f( 1.0F, 1.0F, 1.0F, 1.0F );
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
        
        if( tile.count > 0 ) {
            GL11.glPushMatrix();
            GL11.glAlphaFunc( 516, 0.003921569F );
            GL11.glEnable( 3042 );
            GL11.glBlendFunc( 770, 1 );
            GL11.glDepthMask( false );
            float alpha = MathHelper.sin( (float) Minecraft.getMinecraft().renderViewEntity.ticksExisted / 8.0F ) * 0.1F + 0.5F;
            UtilsFX.bindTexture( BUBBLE_TEXTURE );
            UtilsFX.renderFacingQuad( (double) tile.xCoord + 0.5, (double) tile.yCoord + 1.5, (double) tile.zCoord + 0.5, 0.0F, 0.9F, (float) tile.count / 37.0F * alpha, 1, 0, partialTick, lock == 1 ? 16777215 : 16729156 );
            GL11.glDepthMask( true );
            GL11.glDisable( 3042 );
            GL11.glAlphaFunc( 516, 0.1F );
            GL11.glPopMatrix();
        }
    }
    
    @Override
    public void renderTileEntityAt( TileEntity tileEntity, double x, double y, double z, float partialTick ) {
        renderTileEntityAt( (NodeAgitatorTileEntity) tileEntity, x, y, z, partialTick );
    }
}
