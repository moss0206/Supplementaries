package net.mehvahdjukaar.supplementaries.common.block.blocks;

import net.mehvahdjukaar.selene.blocks.WaterBlock;
import net.mehvahdjukaar.selene.util.WoodSetType;
import net.mehvahdjukaar.supplementaries.common.block.BlockProperties;
import net.mehvahdjukaar.supplementaries.common.block.BlockProperties.BlockAttachment;
import net.mehvahdjukaar.supplementaries.common.block.BlockProperties.SignAttachment;
import net.mehvahdjukaar.supplementaries.common.block.tiles.HangingSignBlockTile;
import net.mehvahdjukaar.supplementaries.common.block.tiles.SwayingBlockTile;
import net.mehvahdjukaar.supplementaries.common.block.util.BlockUtils;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;


public class HangingSignBlock extends WaterBlock implements EntityBlock {
    protected static final VoxelShape SHAPE_Z = Block.box(7, 0, 0, 9, 16, 16);
    protected static final VoxelShape SHAPE_X = Block.box(0, 0, 7, 16, 16, 9);

    public static final EnumProperty<SignAttachment> ATTACHMENT = BlockProperties.SIGN_ATTACHMENT;
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public final WoodSetType woodType;

    public HangingSignBlock(Properties properties, WoodSetType woodType) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false)
                .setValue(ATTACHMENT, SignAttachment.BLOCK_BLOCK).
                setValue(AXIS, Direction.Axis.Z));
        this.woodType = woodType;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand handIn,
                                 BlockHitResult hit) {
        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof HangingSignBlockTile tile && tile.isAccessibleBy(player)) {
                ItemStack handItem = player.getItemInHand(handIn);

                InteractionResult result = tile.textHolder.playerInteract(level, pos, player, handIn, tile);
                if (result != InteractionResult.PASS) return result;

                //place item
                //TODO: fix left hand(shield)
                if (handIn == InteractionHand.MAIN_HAND) {
                    //remove
                    if(!tile.isEmpty()){
                        if( handItem.isEmpty()) {
                            ItemStack it = tile.removeStackFromSlot(0);

                            player.setItemInHand(handIn, it);
                            tile.setChanged();

                            return InteractionResult.CONSUME;
                        }
                    }
                    //place or interact
                    else {
                        //place
                        if (!handItem.isEmpty()) {
                            ItemStack it = handItem.copy();
                            it.setCount(1);
                            tile.setItems(NonNullList.withSize(1, it));

                            if (!player.isCreative()) {
                                handItem.shrink(1);
                            }
                            level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.10F + 0.95F);

                            tile.setChanged();
                        }

                        // open gui (edit sign with empty hand)
                        else {
                            tile.sendOpenGuiPacket(level, pos, player);
                        }
                        return InteractionResult.CONSUME;
                    }
                }
            }
            return InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader worldIn, BlockPos pos) {
        SignAttachment attachment = state.getValue(ATTACHMENT);
        if (attachment == SignAttachment.CEILING) {
            return worldIn.getBlockState(pos.above()).isFaceSturdy(worldIn, pos.above(), Direction.DOWN);
        } else {
            Direction.Axis axis = state.getValue(AXIS);
            if (axis == Direction.Axis.X) {
                return worldIn.getBlockState(pos.relative(Direction.EAST)).getMaterial().isSolid() ||
                        worldIn.getBlockState(pos.relative(Direction.WEST)).getMaterial().isSolid();
            } else {
                return worldIn.getBlockState(pos.relative(Direction.NORTH)).getMaterial().isSolid() ||
                        worldIn.getBlockState(pos.relative(Direction.SOUTH)).getMaterial().isSolid();
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn,
                                  BlockPos currentPos, BlockPos facingPos) {
        super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
        if (facing == Direction.DOWN) return stateIn;
        var attachment = stateIn.getValue(ATTACHMENT);
        if (attachment == SignAttachment.CEILING) {
            if (facing == Direction.UP) {
                return !stateIn.canSurvive(worldIn, currentPos)
                        ? Blocks.AIR.defaultBlockState()
                        : super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
            }
            return stateIn;
        } else {
            return facing.getAxis() == stateIn.getValue(AXIS) ? !stateIn.canSurvive(worldIn, currentPos)
                    ? Blocks.AIR.defaultBlockState()
                    : getConnectedState(stateIn, facingState, worldIn, facingPos, facing.getOpposite()) : stateIn;
        }
    }

    //always returns a not null blockstate.
    public static BlockState getConnectedState(BlockState state, BlockState facingState, LevelAccessor world, BlockPos pos, Direction clickedFace) {
        BlockAttachment attachment = BlockAttachment.get(facingState, pos, world, clickedFace);
        SignAttachment old = state.getValue(ATTACHMENT);
        return state.setValue(ATTACHMENT, old.withAttachment(
                clickedFace.getAxisDirection() == Direction.AxisDirection.NEGATIVE, attachment));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? SHAPE_X : SHAPE_Z;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        //always model cause I need dynamic thingie
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ATTACHMENT);
        builder.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        BlockState s = super.getStateForPlacement(context);
        if (s == null) return null;
        Direction clickedFace = context.getClickedFace();
        // Direction[] lookingDirections = new Direction[]{clickedFace, clickedFace.getOpposite(), Direction.UP, Direction.DOWN};
        Direction.Axis axis = clickedFace.getAxis();
        if (clickedFace.getAxis() == Direction.Axis.Y) {
            axis = context.getHorizontalDirection().getCounterClockWise().getAxis();
            s = s.setValue(AXIS, axis);
            if (clickedFace == Direction.DOWN) {
                s = s.setValue(ATTACHMENT, SignAttachment.CEILING);
                return s;
            }
        } else s = s.setValue(AXIS, axis);

        BlockPos blockpos = context.getClickedPos();
        Level world = context.getLevel();

        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir.getAxis() == axis) {
                BlockPos relative = blockpos.relative(dir.getOpposite());
                BlockState facingState = world.getBlockState(relative);
                s = getConnectedState(s, facingState, world, relative, dir);
            }
        }
        return s;
    }

    @Override
    public boolean isPossibleToRespawnInThis() {
        return true;
    }

    @Override
    public BlockPathTypes getAiPathNodeType(BlockState state, BlockGetter world, BlockPos pos, Mob entity) {
        return BlockPathTypes.OPEN;
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (world.getBlockEntity(pos) instanceof HangingSignBlockTile tile) {
                //InventoryHelper.dropInventoryItems(world, pos, (HangingSignBlockTile) tileentity);

                ItemStack itemstack = tile.getStackInSlot(0);
                ItemEntity itementity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, itemstack);
                itementity.setDefaultPickUpDelay();
                world.addFreshEntity(itementity);
                world.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new HangingSignBlockTile(pPos, pState);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return BlockUtils.getTicker(pBlockEntityType, ModRegistry.HANGING_SIGN_TILE.get(), pLevel.isClientSide ? HangingSignBlockTile::clientTick : null);
    }

    @Override
    public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        BlockUtils.addOptionalOwnership(placer, worldIn, pos);
    }

    @Override
    public BlockState rotate(BlockState pState, Rotation pRot) {
        if (pRot != Rotation.CLOCKWISE_180) {
            return pState.cycle(AXIS);
        }
        return pState;
    }

    @Override
    public void entityInside(BlockState state, Level world, BlockPos pos, Entity entity) {
        super.entityInside(state, world, pos, entity);
        if (world.getBlockEntity(pos) instanceof SwayingBlockTile tile) {
            tile.hitByEntity(entity, state);
        }
    }
}

