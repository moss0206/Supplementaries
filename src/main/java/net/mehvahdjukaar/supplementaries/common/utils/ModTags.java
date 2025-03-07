package net.mehvahdjukaar.supplementaries.common.utils;

import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.common.ForgeTagHandler;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ForgeRegistryTagsProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.GameData;

public class ModTags {

    //block tags
    public static final Tags.IOptionalNamedTag<Block> POSTS = blockTag("posts");
    public static final Tags.IOptionalNamedTag<Block> ENCHANTMENT_BYPASS = blockTag("enchantment_bypass");
    public static final Tags.IOptionalNamedTag<Block> PALISADES = blockTag("palisades");
    public static final Tags.IOptionalNamedTag<Block> BEAMS = blockTag("beams");
    public static final Tags.IOptionalNamedTag<Block> WALLS = blockTag("walls");
    public static final Tags.IOptionalNamedTag<Block> ROPE_SUPPORT_TAG = blockTag("rope_support");
    public static final Tags.IOptionalNamedTag<Block> ROPE_HANG_TAG = blockTag("hang_from_ropes");
    public static final Tags.IOptionalNamedTag<Block> BELLOWS_TICKABLE_TAG = blockTag("bellows_tickable");
    public static final Tags.IOptionalNamedTag<Block> WATER_HOLDER = blockTag("water_holder");
    public static final Tags.IOptionalNamedTag<Block> POURING_TANK = blockTag("pouring_tank");
    public static final Tags.IOptionalNamedTag<Block> WALL_LANTERNS = blockTag("wall_lanterns");
    public static final Tags.IOptionalNamedTag<Block> VINE_SUPPORT = blockTag("vine_support");
    public static final Tags.IOptionalNamedTag<Block> PANE_CONNECTION = blockTag("pane_connection");
    public static final Tags.IOptionalNamedTag<Block> CONCRETE_POWDERS = blockTag("concrete_powders");
    public static final Tags.IOptionalNamedTag<Block> ROTATION_BLACKLIST = blockTag("un_rotatable");
    public static final Tags.IOptionalNamedTag<Block> BOMB_BREAKABLE = blockTag("bomb_breakable");
    //item tags
    public static final Tags.IOptionalNamedTag<Item> SHULKER_BLACKLIST_TAG = itemTag("shulker_blacklist");
    public static final Tags.IOptionalNamedTag<Item> COOKIES = itemTag("cookies");
    public static final Tags.IOptionalNamedTag<Item> BRICKS = itemTag("throwable_bricks");
    public static final Tags.IOptionalNamedTag<Item> ROPES = itemTag("ropes");
    public static final Tags.IOptionalNamedTag<Item> CHAINS = itemTag("chains");
    public static final Tags.IOptionalNamedTag<Item> PEDESTAL_UPRIGHT = itemTag("pedestal_upright");
    public static final Tags.IOptionalNamedTag<Item> PEDESTAL_DOWNRIGHT = itemTag("pedestal_downright");
    public static final Tags.IOptionalNamedTag<Item> CHOCOLATE_BARS = itemTag("chocolate_bars");
    public static final Tags.IOptionalNamedTag<Item> FIRE_SOURCES = itemTag("fire_sources");
    public static final Tags.IOptionalNamedTag<Item> FLOWER_BOX_PLANTABLE = itemTag("flower_box_plantable");
    public static final Tags.IOptionalNamedTag<Item> CHALK = itemTag("chalk");
    public static final Tags.IOptionalNamedTag<Item> BOOKS = itemTag("placeable_books");
    public static final Tags.IOptionalNamedTag<Item> DUSTS = itemTag("hourglass_dusts");
    public static final Tags.IOptionalNamedTag<Item> SANDS = itemTag("hourglass_sands");
    public static final Tags.IOptionalNamedTag<Item> KEY = itemTag("key");
    public static final Tags.IOptionalNamedTag<Item> STATUE_SWORDS = itemTag("statue_swords");
    public static final Tags.IOptionalNamedTag<Item> STATUE_TOOLS = itemTag("statue_tools");
    public static final Tags.IOptionalNamedTag<Item> FLAGS = itemTag("flags");
    public static final Tags.IOptionalNamedTag<Item> PRESENTS = itemTag("presents");
    public static final Tags.IOptionalNamedTag<Item> SHULKER_BOXES = ItemTags.createOptional(new ResourceLocation("forge", "shulker_boxes"));
    //entity tags
    public static final Tags.IOptionalNamedTag<EntityType<?>> JAR_CATCHABLE = entityTag("jar_catchable");
    public static final Tags.IOptionalNamedTag<EntityType<?>> TINTED_JAR_CATCHABLE = entityTag("jar_tinted_catchable");
    public static final Tags.IOptionalNamedTag<EntityType<?>> CAGE_CATCHABLE = entityTag("cage_catchable");
    public static final Tags.IOptionalNamedTag<EntityType<?>> CAGE_BABY_CATCHABLE = entityTag("cage_baby_catchable");
    public static final Tags.IOptionalNamedTag<EntityType<?>> FLUTE_PET = entityTag("flute_pet");
    public static final Tags.IOptionalNamedTag<EntityType<?>> EATS_FODDER = entityTag("eats_fodder");

    public static final Tag.Named<StructureFeature<?>> VILLAGES = structureTag("villages");

    private static Tags.IOptionalNamedTag<StructureFeature<?>> structureTag(String name) {
        return ForgeTagHandler.createOptionalTag(ForgeRegistries.STRUCTURE_FEATURES,Supplementaries.res( name));
    }
    private static Tags.IOptionalNamedTag<Item> itemTag(String name) {
        return ItemTags.createOptional(Supplementaries.res( name));
    }
    private static Tags.IOptionalNamedTag<Block> blockTag(String name) {
        return BlockTags.createOptional(Supplementaries.res( name));
    }
    private static Tags.IOptionalNamedTag<EntityType<?>> entityTag(String name) {
        return EntityTypeTags.createOptional(Supplementaries.res( name));
    }

}
