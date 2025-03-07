package net.mehvahdjukaar.supplementaries.client.renderers.tiles;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.mehvahdjukaar.supplementaries.client.renderers.CapturedMobCache;
import net.mehvahdjukaar.supplementaries.client.renderers.RotHlpr;
import net.mehvahdjukaar.supplementaries.common.block.blocks.PedestalBlock;
import net.mehvahdjukaar.supplementaries.common.block.tiles.GlobeBlockTile;
import net.mehvahdjukaar.supplementaries.common.block.tiles.PedestalBlockTile;
import net.mehvahdjukaar.supplementaries.common.configs.ClientConfigs;
import net.mehvahdjukaar.supplementaries.common.utils.CommonUtil;
import net.mehvahdjukaar.supplementaries.setup.ClientRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;


public class PedestalBlockTileRenderer implements BlockEntityRenderer<PedestalBlockTile> {
    private final Minecraft minecraft = Minecraft.getInstance();
    private final ItemRenderer itemRenderer;
    private final EntityRenderDispatcher entityRenderer;
    private final Font font;

    public PedestalBlockTileRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = minecraft.getItemRenderer();
        this.entityRenderer = minecraft.getEntityRenderDispatcher();
        this.font = minecraft.font;
    }

    protected boolean canRenderName(PedestalBlockTile tile) {
        if (Minecraft.renderNames() && tile.getItem(0).hasCustomHoverName() && !tile.type.isGlobe()) {
            double d0 = entityRenderer.distanceToSqr(tile.getBlockPos().getX() + 0.5, tile.getBlockPos().getY() + 0.5, tile.getBlockPos().getZ() + 0.5);
            return d0 < 16 * 16;
        }
        return false;
    }

    protected void renderName(Component displayNameIn, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {

        double f = 0.875; //height
        int i = 0;

        matrixStackIn.pushPose();

        matrixStackIn.translate(0, f, 0);
        matrixStackIn.mulPose(entityRenderer.cameraOrientation());
        matrixStackIn.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f matrix4f = matrixStackIn.last().pose();
        float f1 = minecraft.options.getBackgroundOpacity(0.25F);
        int j = (int) (f1 * 255.0F) << 24;

        float f2 = (float) (-font.width(displayNameIn) / 2);

        font.drawInBatch(displayNameIn, f2, (float) i, -1, false, matrix4f, bufferIn, false, j, packedLightIn);
        matrixStackIn.popPose();

    }

    @Override
    public void render(PedestalBlockTile tile, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn,
                       int combinedOverlayIn) {
        if (!tile.isEmpty()) {
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.5, 1.125, 0.5);

            if (this.canRenderName(tile)) {
                Component name = tile.getItem(0).getHoverName();
                int i = "Dinnerbone".equals(name.getString()) ? -1 : 1;
                matrixStackIn.scale(i, i, 1);
                this.renderName(name, matrixStackIn, bufferIn, combinedLightIn);
            }
            matrixStackIn.scale(0.5f, 0.5f, 0.5f);
            matrixStackIn.translate(0, 0.25, 0);

            if (tile.getBlockState().getValue(PedestalBlock.AXIS) == Direction.Axis.X) {
                matrixStackIn.mulPose(RotHlpr.Y90);
            }

            ItemTransforms.TransformType transform = ItemTransforms.TransformType.FIXED;
            ItemStack stack = tile.getDisplayedItem();

            if(ClientConfigs.cached.PEDESTAL_SPECIAL){
                switch (tile.type){
                    case SWORD -> {
                        matrixStackIn.translate(0, -0.03125, 0);
                        matrixStackIn.scale(1.5f, 1.5f, 1.5f);
                        matrixStackIn.mulPose(RotHlpr.Z135);
                    }
                    case TRIDENT -> {
                        matrixStackIn.translate(0, 0.03125, 0);
                        matrixStackIn.scale(1.5f, 1.5f, 1.5f);
                        matrixStackIn.mulPose(RotHlpr.ZN45);
                    }
                    case CRYSTAL -> {
                        entityRenderer.render(CapturedMobCache.pedestalCrystal.get(), 0.0D, 0.0D, 0.0D, 0.0F, partialTicks, matrixStackIn, bufferIn, combinedLightIn);
                        matrixStackIn.popPose();
                        return;
                    }
                    default -> {

                        if (ClientConfigs.cached.PEDESTAL_SPIN) {
                            matrixStackIn.translate(0, 6 / 16f, 0);
                            matrixStackIn.scale(1.5f, 1.5f, 1.5f);

                            //BlockPos blockpos = tile.getPos();
                            //long blockoffset = (long) (blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13);

                            //long time = System.currentTimeMillis();

                            //float tt = tile.getLevel().getGameTime() +partialTicks;
                            //float tt = tile.counter + partialTicks;

                            //float tt = ((float)Math.floorMod(tile.getLevel().getGameTime(), 1000L) + partialTicks) / 1000.0F;

                            //long t = blockoffset + time;
                            float angle = (tile.getLevel().getGameTime() % 360) * (float) ClientConfigs.cached.PEDESTAL_SPEED + partialTicks ;
                            Quaternion rotation = Vector3f.YP.rotationDegrees(angle);

                            matrixStackIn.mulPose(rotation);
                        }

                        if(tile.type.isGlobe()){
                            if(ClientRegistry.GLOBE_RENDERER_INSTANCE != null){

                                boolean sepia = tile.type == PedestalBlockTile.DisplayType.SEPIA_GLOBE;
                                Pair<GlobeBlockTile.GlobeModel, ResourceLocation> pair =
                                        stack.hasCustomHoverName() ?
                                                GlobeBlockTile.GlobeType.getGlobeTexture(stack.getHoverName().getString()) :
                                                Pair.of(GlobeBlockTile.GlobeModel.GLOBE, null);

                                ClientRegistry.GLOBE_RENDERER_INSTANCE.renderGlobe(pair, matrixStackIn, bufferIn,
                                        combinedLightIn, combinedOverlayIn, sepia, tile.getLevel());
                            }
                            matrixStackIn.popPose();
                            return;
                        }
                    }
                }
            }


            if (CommonUtil.FESTIVITY.isAprilsFool()) stack = new ItemStack(Items.DIRT);
            this.itemRenderer.renderStatic(stack, transform, combinedLightIn, combinedOverlayIn, matrixStackIn, bufferIn,0);

            matrixStackIn.popPose();
        }
    }
}