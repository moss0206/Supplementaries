package net.mehvahdjukaar.supplementaries.common.block.blocks;

import net.mehvahdjukaar.supplementaries.common.entities.FallingLanternEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class LightableLanternBlock extends LanternBlock {
    public static final VoxelShape SHAPE_DOWN = Shapes.or(Block.box(5.0D, 0.0D, 5.0D, 11.0D, 8.0D, 11.0D),
            Block.box(6.0D, 8.0D, 6.0D, 10.0D, 9.0D, 10.0D));
    public static final VoxelShape SHAPE_UP = Shapes.or(Block.box(5.0D, 5.0D, 5.0D, 11.0D, 13.0D, 11.0D),
            Block.box(6.0D, 13.0D, 6.0D, 10.0D, 14.0D, 10.0D));

    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LightableLanternBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false).setValue(LIT, true)
                .setValue(HANGING, false));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter p_153475_, BlockPos p_153476_, CollisionContext p_153477_) {
        return state.getValue(HANGING) ? SHAPE_UP : SHAPE_DOWN;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // if (state.getValue(HANGING)) return RenderShape.ENTITYBLOCK_ANIMATED;
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        var optional = toggleLight(state, worldIn, pos, player, handIn);
        if (optional.isPresent()) {
            if (!worldIn.isClientSide) {
                worldIn.setBlockAndUpdate(pos, optional.get());
            }
            return InteractionResult.sidedSuccess(worldIn.isClientSide);
        }
        return InteractionResult.PASS;
    }


    public static Optional<BlockState> toggleLight(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn) {
        if (player.getAbilities().mayBuild && handIn == InteractionHand.MAIN_HAND) {
            ItemStack item = player.getItemInHand(handIn);
            if (!state.getValue(LIT)) {
                if (item.getItem() instanceof FlintAndSteelItem) {

                    worldIn.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, worldIn.getRandom().nextFloat() * 0.4F + 0.8F);
                    state = state.setValue(LIT, true);

                    item.hurtAndBreak(1, player, (playerIn) -> playerIn.broadcastBreakEvent(handIn));
                    return Optional.of(state);
                } else if (item.getItem() instanceof FireChargeItem) {

                    worldIn.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1.0F, (worldIn.getRandom().nextFloat() - worldIn.getRandom().nextFloat()) * 0.2F + 1.0F);
                    state = state.setValue(LIT, true);

                    if (!player.isCreative()) item.shrink(1);
                    return Optional.of(state);
                }
            } else if (item.isEmpty()) {

                worldIn.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.5F, 1.5F);
                state = state.setValue(LIT, false);

                return Optional.of(state);
            }
        }
        return Optional.empty();
    }

    // @Nullable
    //@Override
    // public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
    //     return new LightableLanternBlockTile(pPos, pState);
    // }

    //TODO: hitting sounds
    //called by mixin
    public static boolean canSurviveCeiling(BlockState state, BlockPos pos, LevelReader worldIn) {
        if (!RopeBlock.isSupportingCeiling(pos.above(), worldIn) && worldIn instanceof Level l) {
            if (l.getBlockState(pos).is(state.getBlock())) {
                return createFallingLantern(state, pos, l);
            }
            return false;
        }
        return true;
    }

    public static boolean createFallingLantern(BlockState state, BlockPos pos, Level level) {
        if (FallingBlock.isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
            if (state.hasProperty(LanternBlock.HANGING)) {
                double maxY = state.getShape(level, pos).bounds().maxY;
                state = state.setValue(LanternBlock.HANGING, false);
                double yOffset = maxY - state.getShape(level, pos).bounds().maxY;

                FallingBlockEntity fallingblockentity = new FallingLanternEntity(level, pos, state, yOffset);
                level.addFreshEntity(fallingblockentity);
                return true;
            }
        }
        return false;
    }
}
