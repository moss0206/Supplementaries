package net.mehvahdjukaar.supplementaries.common.utils;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;

import javax.annotation.Nullable;

//TODO: rewrite using new 1.17 code
//utility class that contains block item place functions
public class BlockItemUtils {

    @Nullable
    public static BlockState getPlacementState(BlockPlaceContext context, Block block) {
        BlockState blockstate = block.getStateForPlacement(context);
        return blockstate != null && canPlace(context, blockstate) ? blockstate : null;
    }

    public static boolean canPlace(BlockPlaceContext context, BlockState state) {
        Player player = context.getPlayer();
        CollisionContext collisionContext = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        return (state.canSurvive(context.getLevel(), context.getClickedPos())) && context.getLevel().isUnobstructed(state, context.getClickedPos(), collisionContext);
    }

    private static BlockState updateBlockStateFromTag(BlockPos pos, Level world, ItemStack stack, BlockState state) {
        BlockState blockstate = state;
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            CompoundTag blockStateTag = tag.getCompound("BlockStateTag");
            StateDefinition<Block, BlockState> stateDefinition = state.getBlock().getStateDefinition();

            for(String s : blockStateTag.getAllKeys()) {
                Property<?> property = stateDefinition.getProperty(s);
                if (property != null) {
                    String s1 = blockStateTag.get(s).getAsString();
                    blockstate = updateState(blockstate, property, s1);
                }
            }
        }

        if (blockstate != state) {
            world.setBlock(pos, blockstate, 2);
        }

        return blockstate;
    }

    private static <T extends Comparable<T>> BlockState updateState(BlockState state, Property<T> tProperty, String name) {
        return tProperty.getValue(name).map((p) -> state.setValue(tProperty, p)).orElse(state);
    }

    public static InteractionResult place(BlockPlaceContext context, Block blockToPlace) {
        return place(context, blockToPlace, null);
    }

    public static InteractionResult place(BlockPlaceContext context, Block blockToPlace, @Nullable SoundType placeSound) {
        if (!context.canPlace()) {
            return InteractionResult.FAIL;
        } else {
            BlockState blockstate = getPlacementState(context, blockToPlace);
            if (blockstate == null) {
                return InteractionResult.FAIL;
            } else if (!context.getLevel().setBlock(context.getClickedPos(), blockstate, 11)) {
                return InteractionResult.FAIL;
            } else {
                BlockPos blockpos = context.getClickedPos();
                Level world = context.getLevel();
                Player player = context.getPlayer();
                ItemStack itemstack = context.getItemInHand();
                BlockState placedState = world.getBlockState(blockpos);
                Block block = placedState.getBlock();
                if (block == blockstate.getBlock()) {
                    placedState = updateBlockStateFromTag(blockpos, world, itemstack, placedState);
                    BlockItem.updateCustomBlockEntityTag(world, player, blockpos, itemstack);
                    block.setPlacedBy(world, blockpos, placedState, player, itemstack);
                    if (player instanceof ServerPlayer serverPlayer) {
                        CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, blockpos, itemstack);
                    }
                    world.gameEvent(player, GameEvent.BLOCK_PLACE, blockpos);
                }
                if(placeSound == null) placeSound = placedState.getSoundType(world, blockpos, context.getPlayer());
                world.playSound(player, blockpos, placeSound.getPlaceSound(), SoundSource.BLOCKS, (placeSound.getVolume() + 1.0F) / 2.0F, placeSound.getPitch() * 0.8F);
                if (player == null || !player.getAbilities().instabuild) {
                    itemstack.shrink(1);
                }

                return InteractionResult.sidedSuccess(world.isClientSide);
            }

        }
    }
}
