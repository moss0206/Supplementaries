package net.mehvahdjukaar.supplementaries.dynamicpack;

import com.google.common.base.Stopwatch;
import com.mojang.blaze3d.platform.NativeImage;
import net.mehvahdjukaar.selene.resourcepack.AssetGenerators;
import net.mehvahdjukaar.selene.resourcepack.DynamicTexturePack;
import net.mehvahdjukaar.selene.resourcepack.RPUtils;
import net.mehvahdjukaar.selene.resourcepack.RPUtils.ResType;
import net.mehvahdjukaar.selene.resourcepack.RPUtils.StaticResource;
import net.mehvahdjukaar.selene.resourcepack.VanillaResourceManager;
import net.mehvahdjukaar.selene.textures.Palette;
import net.mehvahdjukaar.selene.textures.Respriter;
import net.mehvahdjukaar.selene.textures.SpriteUtils;
import net.mehvahdjukaar.selene.util.WoodSetType;
import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.client.WallLanternStuff;
import net.mehvahdjukaar.supplementaries.common.configs.ClientConfigs;
import net.mehvahdjukaar.supplementaries.common.configs.RegistryConfigs;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ClientDynamicResourcesHandler implements PreparableReloadListener {

    private static final Logger LOGGER = Supplementaries.LOGGER;
    public static final DynamicTexturePack DYNAMIC_TEXTURE_PACK =
            new DynamicTexturePack(Supplementaries.res("virtual_resourcepack"));

    private boolean firstLoad = false;

    public static void registerBus(IEventBus bus) {
        DYNAMIC_TEXTURE_PACK.registerPack(bus);

        ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager())
                .registerReloadListener(new ClientDynamicResourcesHandler());

        DYNAMIC_TEXTURE_PACK.generateDebugResources = RegistryConfigs.reg.DEBUG_RESOURCES.get();

    }

    private void generateStaticResources(ResourceManager manager) {
        Stopwatch watch = Stopwatch.createStarted();

        //generate static resources

        AssetGenerators.LangBuilder langBuilder = new AssetGenerators.LangBuilder();

        //------hanging signs------
        {

            StaticResource hsBlockState = getResOrLog(manager,
                    RPUtils.resPath(Supplementaries.res("hanging_sign_oak"), ResType.BLOCKSTATES));
            StaticResource hsModel = getResOrLog(manager,
                    RPUtils.resPath(Supplementaries.res("hanging_signs/hanging_sign_oak"), ResType.BLOCK_MODELS));
            StaticResource hsLoader = getResOrLog(manager,
                    RPUtils.resPath(Supplementaries.res("hanging_signs/loader_template"), ResType.BLOCK_MODELS));
            StaticResource hsItemModel = getResOrLog(manager,
                    RPUtils.resPath(Supplementaries.res("hanging_sign_oak"), ResType.ITEM_MODELS));

            for (var e : ModRegistry.HANGING_SIGNS.entrySet()) {
                WoodSetType wood = e.getKey();
                if (true || !wood.isVanilla()) {
                    var v = e.getValue();

                    String id = wood.getVariantId("hanging_sign");
                    langBuilder.addEntry(v, wood.getNameForTranslation("hanging_sign"));

                    try {
                        DYNAMIC_TEXTURE_PACK.addSimilarJsonResource(hsBlockState, "hanging_sign_oak", id);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to generate Hanging Sign blockstate definition for {} : {}", v, ex);
                    }

                    try {
                        DYNAMIC_TEXTURE_PACK.addSimilarJsonResource(hsModel, "hanging_sign_oak", id);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to generate Hanging Sign block model for {} : {}", v, ex);
                    }

                    try {
                        DYNAMIC_TEXTURE_PACK.addSimilarJsonResource(hsItemModel, "hanging_sign_oak", id);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to generate Hanging Sign item model for {} : {}", v, ex);
                    }

                    try {
                        String logTexture;
                        try {
                            logTexture = RPUtils.findFirstBlockTextureLocation(manager, Objects.requireNonNull(wood.logBlock), s -> !s.contains("top"));
                        } catch (Exception e1) {
                            logTexture = RPUtils.findFirstBlockTextureLocation(manager, wood.plankBlock, s -> true);
                            LOGGER.error("Could not properly generate Hanging Sign model for {}. Falling back to planks texture : {}", v, e1);
                        }
                        addHangingSignLoaderModel(Objects.requireNonNull(hsLoader), id, logTexture);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to generate Hanging Sign loader model for {} : {}", v, ex);
                    }
                }
            }
        }


        //------sing posts-----
        {
            StaticResource spItemModel = getResOrLog(manager,
                    RPUtils.resPath(Supplementaries.res("sign_post_oak"), ResType.ITEM_MODELS));

            for (var e : ModRegistry.SIGN_POST_ITEMS.entrySet()) {
                WoodSetType wood = e.getKey();
                if (!wood.isVanilla() || true) {
                    var v = e.getValue();
                    langBuilder.addEntry(v, e.getKey().getNameForTranslation("sign_post"));

                    try {
                        DYNAMIC_TEXTURE_PACK.addSimilarJsonResource(spItemModel,
                                "sign_post_oak", wood.getVariantId("sign_post"));
                    } catch (Exception ex) {
                        LOGGER.error("Failed to generate Sign Post item model for {} : {}", v, ex);
                    }
                }
            }
        }


        DYNAMIC_TEXTURE_PACK.addLang(Supplementaries.res("en_us"), langBuilder.build());

        LOGGER.info("Generated runtime client resources in: {} seconds", watch.elapsed().toSeconds());
    }

    public void addHangingSignLoaderModel(RPUtils.StaticResource resource, String woodTextPath, String logTexture) {
        String string = new String(resource.data, StandardCharsets.UTF_8);

        string = string.replace("wood_type", woodTextPath);
        string = string.replace("log_texture", logTexture);

        //adds modified under my namespace
        ResourceLocation newRes = Supplementaries.res("hanging_signs/" + woodTextPath + "_loader");
        DYNAMIC_TEXTURE_PACK.addBytes(newRes, string.getBytes(), ResType.BLOCK_MODELS);
    }


    //-------------resource pack dependant textures-------------


    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier stage, ResourceManager manager,
                                          ProfilerFiller workerProfiler, ProfilerFiller mainProfiler,
                                          Executor workerExecutor, Executor mainExecutor) {

        WallLanternStuff.onResourceReload(manager);

        boolean resourcePackSupport = ClientConfigs.general.RESOURCE_PACK_SUPPORT.get();
        //manager that only has vanilla resources

        if (!this.firstLoad) {
            this.firstLoad = true;
            generateStaticResources(manager);
            if (!resourcePackSupport) {
                VanillaResourceManager vanillaManager = new VanillaResourceManager();
                this.generateDynamicTextures(vanillaManager);
                vanillaManager.close();
            }
        }

        //generate textures
        if (resourcePackSupport) {
            this.generateDynamicTextures(manager);
        }

        return CompletableFuture.supplyAsync(() -> null, workerExecutor)
                .thenCompose(stage::wait)
                .thenAcceptAsync((noResult) -> {
                }, mainExecutor);
    }


    //I need to close all resources here
    protected void generateDynamicTextures(ResourceManager manager) {

        //hanging signs block textures
        try (NativeImage template = readImage(manager, Supplementaries.res(
                "textures/blocks/hanging_signs/hanging_sign_oak.png"));
             NativeImage mask = readImage(manager, Supplementaries.res(
                     "textures/blocks/hanging_signs/board_mask.png"))) {

            Palette palette = Palette.fromImage(template, mask);
            Respriter respriter = new Respriter(template, palette);

            for (var e : ModRegistry.HANGING_SIGNS.entrySet()) {
                WoodSetType wood = e.getKey();
                //if (wood.isVanilla()) continue;
                ResourceLocation textureRes = Supplementaries.res(
                        String.format("blocks/hanging_signs/%s", wood.getVariantId("hanging_sign")));
                if (hasHandmadeTexture(manager, textureRes)) continue;
                var v = e.getValue();
                try (NativeImage plankPalette = RPUtils.findFirstBlockTexture(manager, wood.plankBlock)) {

                    Palette targetPalette = SpriteUtils.extrapolateSignBlockPalette(plankPalette);
                    NativeImage newImage = respriter.recolorImage(targetPalette);

                    DYNAMIC_TEXTURE_PACK.addTexture(textureRes, newImage);
                } catch (Exception ex) {
                    LOGGER.error("Failed to generate Hanging Sign block texture for for {} : {}", v, ex);
                }

            }
        } catch (Exception ex) {
            LOGGER.error("Could not generate any Hanging Sign block texture : ", ex);
        }

        //hanging sign item textures
        try (NativeImage boardTemplate = readImage(manager, Supplementaries.res(
                "textures/items/hanging_signs/template.png"));
             NativeImage boardMask = readImage(manager, Supplementaries.res(
                     "textures/items/hanging_signs/board_mask.png"))) {

            Palette palette = Palette.fromImage(boardTemplate, boardMask);
            Respriter respriter = new Respriter(boardTemplate, palette);

            for (var e : ModRegistry.HANGING_SIGNS.entrySet()) {
                WoodSetType wood = e.getKey();
                //if (wood.isVanilla()) continue;
                ResourceLocation textureRes = Supplementaries.res(String.format("items/hanging_signs/%s", wood.getVariantId("hanging_sign")));
                if (hasHandmadeTexture(manager, textureRes)) continue;
                var v = e.getValue();

                NativeImage newImage = null;
                if (wood.signItem != null) {
                    try (NativeImage vanillaSign = RPUtils.findFirstItemTexture(manager, wood.signItem.get());
                         NativeImage signMask = readImage(manager, Supplementaries.res(
                                 "textures/items/hanging_signs/sign_board_mask.png"))) {

                        Palette targetPalette = Palette.fromImage(vanillaSign, signMask);
                        newImage = respriter.recolorImage(targetPalette);

                        try (NativeImage scribbles = recolorFromVanilla(manager, vanillaSign,
                                Supplementaries.res("textures/items/hanging_signs/sign_scribbles_mask.png"),
                                Supplementaries.res("textures/items/hanging_signs/scribbles_template.png"));) {
                            SpriteUtils.mergeImages(newImage, scribbles);
                        } catch (Exception ex) {
                            LOGGER.error("Could not properly color Hanging Sign texture for {} : {}", v, ex);
                        }

                        try (NativeImage stick = recolorFromVanilla(manager, vanillaSign,
                                Supplementaries.res("textures/items/hanging_signs/sign_stick_mask.png"),
                                Supplementaries.res("textures/items/hanging_signs/stick_template.png"))) {
                            SpriteUtils.mergeImages(newImage, stick);
                        } catch (Exception ex) {
                            LOGGER.error("Could not properly color Hanging Sign item texture for {} : {}", v, ex);
                        }

                    } catch (Exception ex) {
                        LOGGER.error("Could not find sign texture for wood type {}. Using plank texture : {}", wood, ex);
                    }
                }
                //if it failed use plank one
                if (newImage == null) {
                    try (NativeImage plankPalette = RPUtils.findFirstBlockTexture(manager, wood.plankBlock)) {
                        Palette targetPalette = SpriteUtils.extrapolateWoodItemPalette(plankPalette);
                        newImage = respriter.recolorImage(targetPalette);
                    } catch (Exception ex) {
                        LOGGER.error("Failed to generate Hanging Sign item texture for for {} : {}", v, ex);
                    }
                }
                if (newImage != null) {
                    DYNAMIC_TEXTURE_PACK.addTexture(textureRes, newImage);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not generate any Hanging Sign item texture : ", ex);
        }

        //sign posts item textures
        try (NativeImage template = readImage(manager, Supplementaries.res(
                "textures/items/sign_posts/template.png"))) {

            Respriter respriter = new Respriter(template);

            for (var e : ModRegistry.SIGN_POST_ITEMS.entrySet()) {
                WoodSetType wood = e.getKey();
                //if (wood.isVanilla()) continue;
                ResourceLocation textureRes = Supplementaries.res(
                        String.format("items/sign_posts/%s", wood.getVariantId("sign_post")));
                if (hasHandmadeTexture(manager, textureRes)) continue;
                var v = e.getValue();

                NativeImage newImage = null;
                if (wood.signItem != null) {
                    try (NativeImage vanillaSign = RPUtils.findFirstItemTexture(manager, wood.signItem.get());
                         NativeImage signMask = readImage(manager, Supplementaries.res(
                                 "textures/items/hanging_signs/sign_board_mask.png"))) {

                        Palette targetPalette = Palette.fromImage(vanillaSign, signMask);
                        newImage = respriter.recolorImage(targetPalette);

                        try (NativeImage scribbles = recolorFromVanilla(manager, vanillaSign,
                                Supplementaries.res("textures/items/hanging_signs/sign_scribbles_mask.png"),
                                Supplementaries.res("textures/items/sign_posts/scribbles_template.png"));) {
                            SpriteUtils.mergeImages(newImage, scribbles);
                        } catch (Exception ex) {
                            LOGGER.error("Could not properly color Sign Post item texture for {} : {}", v, ex);
                        }

                    } catch (Exception ex) {
                        LOGGER.error("Could not find sign texture for wood type {}. Using plank texture : {}", wood, ex);
                    }
                }
                //if it failed use plank one
                if (newImage == null) {
                    try (NativeImage plankPalette = RPUtils.findFirstBlockTexture(manager, wood.plankBlock)) {
                        Palette targetPalette = SpriteUtils.extrapolateWoodItemPalette(plankPalette);
                        newImage = respriter.recolorImage(targetPalette);

                    } catch (Exception ex) {
                        LOGGER.error("Failed to generate Sign Post item texture for for {} : {}", v, ex);
                    }
                }
                if (newImage != null) {

                    DYNAMIC_TEXTURE_PACK.addTexture(textureRes, newImage);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not generate any Sign Post item texture : ", ex);
        }

        //sign posts block textures
        try (NativeImage template = readImage(manager, Supplementaries.res(
                "textures/entity/sign_posts/sign_post_oak.png"))) {

            Respriter respriter = new Respriter(template);

            for (var e : ModRegistry.SIGN_POST_ITEMS.entrySet()) {
                WoodSetType wood = e.getKey();
                var textureRes = Supplementaries.res(String.format("entity/sign_posts/%s", wood.getVariantId("sign_post")));
                if (hasHandmadeTexture(manager, textureRes)) continue;
                //if (wood.isVanilla()) continue;
                var v = e.getValue();

                try (NativeImage plankPalette = RPUtils.findFirstBlockTexture(manager, wood.plankBlock)) {
                    NativeImage newImage = respriter.recolorImage(plankPalette, null);

                    DYNAMIC_TEXTURE_PACK.addTexture(textureRes, newImage);
                } catch (Exception ex) {
                    LOGGER.error("Failed to generate Sign Post block texture for for {} : {}", v, ex);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not generate any Sign Post block texture : ", ex);
        }
    }

    private boolean hasHandmadeTexture(ResourceManager manager, ResourceLocation res) {
        ResourceLocation fullRes = RPUtils.resPath(res, ResType.TEXTURES);
        if (manager.hasResource(fullRes)) {
            try {
                var r = manager.getResource(fullRes);
                return !r.getSourceName().equals(DYNAMIC_TEXTURE_PACK.getName());
            } catch (IOException ignored) {
            }
        }
        return false;
    }
//TODO: invert scribble color if sign is darker than them

    /**
     * recolors the template image with the color grabbed from the given image restrained to its mask, if possible
     */
    @Nullable
    private NativeImage recolorFromVanilla(ResourceManager manager, NativeImage vanillaTexture, ResourceLocation vanillaMask,
                                           ResourceLocation templateTexture) {
        try {
            NativeImage scribbleMask = readImage(manager, vanillaMask);
            NativeImage scribbleTemplate = readImage(manager, templateTexture);
            Respriter scribbleRespriter = new Respriter(scribbleTemplate);
            return scribbleRespriter.recolorImage(vanillaTexture, scribbleMask);
        } catch (Exception ignored) {
        }
        return null;
    }


    //helpers

    private NativeImage readImage(ResourceManager manager, ResourceLocation resourceLocation) throws IOException {
        return NativeImage.read(manager.getResource(resourceLocation).getInputStream());
    }

    @Nullable
    private StaticResource getResOrLog(ResourceManager manager, ResourceLocation location) {
        try {
            return new StaticResource(manager.getResource(location));
        } catch (Exception e) {
            Supplementaries.LOGGER.error("Could not find resource {} while generating dynamic resource pack", location);
        }
        return null;
    }

}
