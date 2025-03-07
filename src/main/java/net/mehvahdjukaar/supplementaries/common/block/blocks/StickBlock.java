package net.mehvahdjukaar.supplementaries.common.block.blocks;

import com.google.common.collect.ImmutableMap;
import net.mehvahdjukaar.selene.blocks.WaterBlock;
import net.mehvahdjukaar.supplementaries.api.IRotatable;
import net.mehvahdjukaar.supplementaries.common.block.BlockProperties;
import net.mehvahdjukaar.supplementaries.common.block.tiles.FlagBlockTile;
import net.mehvahdjukaar.supplementaries.common.configs.ServerConfigs;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class StickBlock extends WaterBlock implements IRotatable { // IRotationLockable,
    protected static final VoxelShape Y_AXIS_AABB = Block.box(7D, 0.0D, 7D, 9D, 16.0D, 9D);
    protected static final VoxelShape Z_AXIS_AABB = Block.box(7D, 7D, 0.0D, 9D, 9D, 16.0D);
    protected static final VoxelShape X_AXIS_AABB = Block.box(0.0D, 7D, 7D, 16.0D, 9D, 9D);
    protected static final VoxelShape Y_Z_AXIS_AABB = Shapes.or(Block.box(7D, 0.0D, 7D, 9D, 16.0D, 9D),
            Block.box(7D, 7D, 0.0D, 9D, 9D, 16.0D));
    protected static final VoxelShape Y_X_AXIS_AABB = Shapes.or(Block.box(7D, 0.0D, 7D, 9D, 16.0D, 9D),
            Block.box(0.0D, 7D, 7D, 16.0D, 9D, 9D));
    protected static final VoxelShape X_Z_AXIS_AABB = Shapes.or(Block.box(7D, 7D, 0.0D, 9D, 9D, 16.0D),
            Block.box(0.0D, 7D, 7D, 16.0D, 9D, 9D));
    protected static final VoxelShape X_Y_Z_AXIS_AABB = Shapes.or(Block.box(7D, 7D, 0.0D, 9D, 9D, 16.0D),
            Block.box(0.0D, 7D, 7D, 16.0D, 9D, 9D),
            Block.box(7D, 0.0D, 7D, 9D, 16.0D, 9D));

    public static final BooleanProperty AXIS_X = BlockProperties.AXIS_X;
    public static final BooleanProperty AXIS_Y = BlockProperties.AXIS_Y;
    public static final BooleanProperty AXIS_Z = BlockProperties.AXIS_Z;

    protected final Map<Direction.Axis, BooleanProperty> AXIS2PROPERTY = ImmutableMap.of(Direction.Axis.X, AXIS_X, Direction.Axis.Y, AXIS_Y, Direction.Axis.Z, AXIS_Z);

    private final int fireSpread;

    public StickBlock(Properties properties, int fireSpread) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, Boolean.FALSE).setValue(AXIS_Y, true).setValue(AXIS_X, false).setValue(AXIS_Z, false));
        this.fireSpread = fireSpread;
    }

    public StickBlock(Properties properties) {
        this(properties, 60);
    }


    @Override
    public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return fireSpread;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face) {
        return fireSpread;
    }

    @Override
    public void appendHoverText(ItemStack stack, BlockGetter worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        tooltip.add((new TextComponent("You shouldn't have this")).withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(AXIS_X, AXIS_Y, AXIS_Z);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter reader, BlockPos pos, CollisionContext context) {
        boolean x = state.getValue(AXIS_X);
        boolean y = state.getValue(AXIS_Y);
        boolean z = state.getValue(AXIS_Z);
        if (x) {
            if (y) {
                if (z) return X_Y_Z_AXIS_AABB;
                return Y_X_AXIS_AABB;
            } else if (z) return X_Z_AXIS_AABB;
            return X_AXIS_AABB;
        }
        if (z) {
            if (y) return Y_Z_AXIS_AABB;
            return Z_AXIS_AABB;
        }
        return Y_AXIS_AABB;
    }

    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos());
        BooleanProperty axis = AXIS2PROPERTY.get(context.getClickedFace().getAxis());
        if (blockstate.is(this)) {
            return blockstate.setValue(axis, true);
        } else {
            return super.getStateForPlacement(context).setValue(AXIS_Y, false).setValue(axis, true);
        }
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.isSecondaryUseActive() && context.getItemInHand().is(this.asItem())) {
            BooleanProperty axis = AXIS2PROPERTY.get(context.getClickedFace().getAxis());
            if (!state.getValue(axis)) return true;
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter worldIn, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {

        if (player.getItemInHand(hand).isEmpty() && hand == InteractionHand.MAIN_HAND) {
            if (ServerConfigs.cached.STICK_POLE) {
                if (this.material != Material.WOOD) return InteractionResult.PASS;
                if (world.isClientSide) return InteractionResult.SUCCESS;
                else {
                    Direction moveDir = player.isShiftKeyDown() ? Direction.DOWN : Direction.UP;
                    findConnectedFlag(world, pos, Direction.UP, moveDir, 0);
                    findConnectedFlag(world, pos, Direction.DOWN, moveDir, 0);
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    private static boolean isVertical(BlockState state) {
        return state.getValue(AXIS_Y) && !state.getValue(AXIS_X) && !state.getValue(AXIS_Z);
    }

    public static boolean findConnectedFlag(Level world, BlockPos pos, Direction searchDir, Direction moveDir, int it) {
        if (it > ServerConfigs.cached.STICK_POLE_LENGTH) return false;
        BlockState state = world.getBlockState(pos);
        Block b = state.getBlock();
        if (b == ModRegistry.STICK_BLOCK.get() && isVertical(state)) {
            return findConnectedFlag(world, pos.relative(searchDir), searchDir, moveDir, it + 1);
        } else if (b instanceof FlagBlock && it != 0) {
            BlockPos toPos = pos.relative(moveDir);
            BlockState stick = world.getBlockState(toPos);

            if (world.getBlockEntity(pos) instanceof FlagBlockTile tile && stick.getBlock() == ModRegistry.STICK_BLOCK.get() && isVertical(stick)) {

                world.setBlockAndUpdate(pos, stick);
                world.setBlockAndUpdate(toPos, state);

                CompoundTag tag = tile.saveWithoutMetadata();
                BlockEntity te = world.getBlockEntity(toPos);
                if (te != null) {
                    te.load(tag);
                }
                world.playSound(null, toPos, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1F, 1.4F);
                return true;
            }
        }
        return false;
    }

    //quark
    //TODO: improve for multiple sticks
    //@Override
    public BlockState applyRotationLock(Level world, BlockPos blockPos, BlockState state, Direction dir, int half) {
        int i = 0;
        if (state.getValue(AXIS_X)) i++;
        if (state.getValue(AXIS_Y)) i++;
        if (state.getValue(AXIS_Z)) i++;
        if (i == 1) state.setValue(AXIS_Z, false).setValue(AXIS_X, false)
                .setValue(AXIS_Y, false).setValue(AXIS2PROPERTY.get(dir.getAxis()), true);
        return state;
    }

    @Override
    public Optional<BlockState> getRotatedState(BlockState state, LevelAccessor world, BlockPos pos, Rotation rotation, Direction axis, @org.jetbrains.annotations.Nullable Vec3 hit) {
        boolean x = state.getValue(AXIS_X);
        boolean y = state.getValue(AXIS_Y);
        boolean z = state.getValue(AXIS_Z);
        return Optional.of(switch (axis.getAxis()) {
            case Y -> state.setValue(AXIS_X, z).setValue(AXIS_Z, x);
            case X -> state.setValue(AXIS_Y, z).setValue(AXIS_Z, y);
            case Z -> state.setValue(AXIS_X, y).setValue(AXIS_Y, x);
        });
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootContext.Builder pBuilder) {
        int i = 0;
        if (state.getValue(AXIS_X)) i++;
        if (state.getValue(AXIS_Y)) i++;
        if (state.getValue(AXIS_Z)) i++;
        return List.of(new ItemStack(this.asItem(), i));
    }
}
