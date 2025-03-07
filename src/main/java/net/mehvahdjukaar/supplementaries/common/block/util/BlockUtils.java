package net.mehvahdjukaar.supplementaries.common.block.util;

import net.mehvahdjukaar.selene.blocks.IOwnerProtected;
import net.mehvahdjukaar.supplementaries.api.IRotatable;
import net.mehvahdjukaar.supplementaries.common.block.BlockProperties;
import net.mehvahdjukaar.supplementaries.common.configs.ServerConfigs;
import net.mehvahdjukaar.supplementaries.common.utils.ModTags;
import net.mehvahdjukaar.supplementaries.common.utils.VectorUtils;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

public class BlockUtils {

    public static <T extends Comparable<T>, A extends Property<T>> BlockState replaceProperty(BlockState from, BlockState to, A property) {
        if (from.hasProperty(property)) {
            return to.setValue(property, from.getValue(property));
        }
        return to;
    }

    public static <T extends BlockEntity & IOwnerProtected> void addOptionalOwnership(LivingEntity placer, T tileEntity) {
        if (ServerConfigs.cached.SERVER_PROTECTION && placer instanceof Player) {
            tileEntity.setOwner(placer.getUUID());
        }
    }

    public static void addOptionalOwnership(LivingEntity placer, Level world, BlockPos pos) {
        if (ServerConfigs.cached.SERVER_PROTECTION && placer instanceof Player) {
            if (world.getBlockEntity(pos) instanceof IOwnerProtected tile) {
                tile.setOwner(placer.getUUID());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> getTicker(BlockEntityType<A> type, BlockEntityType<E> targetType, BlockEntityTicker<? super E> ticker) {
        return targetType == type ? (BlockEntityTicker<A>) ticker : null;
    }

    public static class PlayerLessContext extends BlockPlaceContext {
        public PlayerLessContext(Level worldIn, @Nullable Player playerIn, InteractionHand handIn, ItemStack stackIn, BlockHitResult rayTraceResultIn) {
            super(worldIn, playerIn, handIn, stackIn, rayTraceResultIn);
        }
    }

    //rotation stuff
    //returns rotation direction axis which might be different that the clicked face
    public static Optional<Direction> tryRotatingBlockAndConnected(Direction face, boolean ccw, BlockPos targetPos, Level level, Vec3 hit) {
        BlockState state = level.getBlockState(targetPos);
        if (state.getBlock() instanceof IRotatable rotatable) {
            return rotatable.rotateOverAxis(state, level, targetPos, ccw ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90, face, hit);
        }
        Optional<Direction> special = tryRotatingSpecial(face, ccw, targetPos, level, state, hit);
        if (special.isPresent()) return special;
        return tryRotatingBlock(face, ccw, targetPos, level, state, hit);
    }

    public static Optional<Direction> tryRotatingBlock(Direction face, boolean ccw, BlockPos targetPos, Level level, Vec3 hit) {
        return tryRotatingBlock(face, ccw, targetPos, level, level.getBlockState(targetPos), hit);
    }

    // can be called on both sides
    // returns the direction onto which the block was actually rotated
    public static Optional<Direction> tryRotatingBlock(Direction dir, boolean ccw, BlockPos targetPos, Level world, BlockState state, Vec3 hit) {

        //interface stuff
        if (state.getBlock() instanceof IRotatable rotatable) {
            return rotatable.rotateOverAxis(state, world, targetPos, ccw ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90, dir, hit);
        }

        Optional<BlockState> optional = getRotatedState(dir, ccw, targetPos, world, state);
        if (optional.isPresent()) {
            BlockState rotated = optional.get();

            if (rotated.canSurvive(world, targetPos)) {
                rotated = Block.updateFromNeighbourShapes(rotated, world, targetPos);

                if (rotated != state) {
                    if (world instanceof ServerLevel serverLevel) {
                        world.setBlock(targetPos, rotated, 11);
                        //level.updateNeighborsAtExceptFromFacing(pos, newState.getBlock(), mydir.getOpposite());
                    }
                    return Optional.of(dir);
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockState> getRotatedState(Direction dir, boolean ccw, BlockPos targetPos, Level world, BlockState state) {

        // is block blacklisted?
        if (isBlacklisted(state)) return Optional.empty();

        Rotation rot = ccw ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
        Block block = state.getBlock();

        if (state.hasProperty(BlockProperties.FLIPPED)) {
            return Optional.of(state.cycle(BlockProperties.FLIPPED));
        }
        //horizontal facing blocks -easy
        if (dir.getAxis() == Direction.Axis.Y) {

            if (block == Blocks.CAKE) {
                int bites = state.getValue(CakeBlock.BITES);
                if (bites != 0) return Optional.of(ModRegistry.DIRECTIONAL_CAKE.get().defaultBlockState()
                        .setValue(CakeBlock.BITES, bites).rotate(world, targetPos, rot));
            }

            BlockState rotated = state.rotate(world, targetPos, rot);
            //also hardcoding vanilla rotation methods cause some mods just dont implement rotate methods for their blocks
            //this could cause problems for mods that do and dont want it to be rotated but those should really be added to the blacklist
            if (rotated == state) {
                if (state.hasProperty(BlockStateProperties.FACING)) {
                    rotated = state.setValue(BlockStateProperties.FACING,
                            rot.rotate(state.getValue(BlockStateProperties.FACING)));
                } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                    rotated = state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                            rot.rotate(state.getValue(BlockStateProperties.HORIZONTAL_FACING)));
                } else if (state.hasProperty(RotatedPillarBlock.AXIS)) {
                    rotated = RotatedPillarBlock.rotatePillar(state, rot);
                } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                    rotated = state.cycle(BlockStateProperties.HORIZONTAL_AXIS);
                }
            }
            return Optional.of(rotated);
        }
        else if(state.hasProperty(BlockStateProperties.ATTACH_FACE) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)){
            return Optional.of(rotateFaceBlockHorizontal(dir, ccw, state));
        }
        // 6 dir blocks blocks
        if (state.hasProperty(BlockStateProperties.FACING)) {
            return Optional.of(rotateBlockStateOnAxis(state, dir, ccw));
        }
        // axis blocks
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis targetAxis = state.getValue(BlockStateProperties.AXIS);
            Direction.Axis myAxis = dir.getAxis();
            if (myAxis == Direction.Axis.X) {
                return Optional.of(state.setValue(BlockStateProperties.AXIS, targetAxis == Direction.Axis.Y ? Direction.Axis.Z : Direction.Axis.Y));
            } else if (myAxis == Direction.Axis.Z) {
                return Optional.of(state.setValue(BlockStateProperties.AXIS, targetAxis == Direction.Axis.Y ? Direction.Axis.X : Direction.Axis.Y));
            }
        }
        if (block instanceof StairBlock) {
            Direction facing = state.getValue(StairBlock.FACING);
            if (facing.getAxis() == dir.getAxis()) return Optional.empty();

            boolean flipped = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ^ ccw;
            Half half = state.getValue(StairBlock.HALF);
            boolean top = half == Half.TOP;
            boolean positive = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE;

            if ((top ^ positive) ^ flipped) {
                half = top ? Half.BOTTOM : Half.TOP;
            } else {
                facing = facing.getOpposite();
            }

            return Optional.of(state.setValue(StairBlock.HALF, half).setValue(StairBlock.FACING, facing));
        }
        if (state.hasProperty(SlabBlock.TYPE)) {
            SlabType type = state.getValue(SlabBlock.TYPE);
            if (type == SlabType.DOUBLE) return Optional.empty();
            return Optional.of(state.setValue(SlabBlock.TYPE, type == SlabType.BOTTOM ? SlabType.TOP : SlabType.BOTTOM));
        }
        if (state.hasProperty(TrapDoorBlock.HALF)) {
            return Optional.of(state.cycle(TrapDoorBlock.HALF));
        }
        return Optional.empty();
    }

    //check if it has facing property
    private static BlockState rotateBlockStateOnAxis(BlockState state, Direction axis, boolean ccw) {
        Vec3 targetNormal = VectorUtils.ItoD(state.getValue(BlockStateProperties.FACING).getNormal());
        Vec3 myNormal = VectorUtils.ItoD(axis.getNormal());
        if (!ccw) targetNormal = targetNormal.scale(-1);

        Vec3 rotated = myNormal.cross(targetNormal);
        // not on same axis, can rotate
        if (rotated != Vec3.ZERO) {
            Direction newDir = Direction.getNearest(rotated.x(), rotated.y(), rotated.z());
            return state.setValue(BlockStateProperties.FACING, newDir);
        }
        return state;
    }

    private static boolean isBlacklisted(BlockState state) {
        // double blocks
        if (state.getBlock() instanceof BedBlock) return true;
        if (state.hasProperty(BlockStateProperties.CHEST_TYPE)) {
            if (state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) return true;
        }
        // no piston bases
        if (state.hasProperty(BlockStateProperties.EXTENDED)) {
            if (state.getValue(BlockStateProperties.EXTENDED)) return true;
        }
        // nor piston arms
        if (state.hasProperty(BlockStateProperties.SHORT)) return true;

        return state.is(ModTags.ROTATION_BLACKLIST);
    }


    private static Optional<Direction> tryRotatingSpecial(Direction face, boolean ccw, BlockPos pos, Level level, BlockState state, Vec3 hit) {
        Block b = state.getBlock();
        Rotation rot = ccw ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            int r = state.getValue(BlockStateProperties.ROTATION_16);
            r += (ccw ? -1 : 1);
            if (r < 0) r += 16;
            r = r % 16;
            level.setBlock(pos, state.setValue(BlockStateProperties.ROTATION_16, r), 2);
            return Optional.of(Direction.UP);
        }

        if (state.hasProperty(BlockStateProperties.EXTENDED) && state.getValue(BlockStateProperties.EXTENDED)) {
            if (state.hasProperty(PistonHeadBlock.FACING)) {
                BlockState newBase = rotateBlockStateOnAxis(state, face, ccw);
                BlockPos headPos = pos.relative(state.getValue(PistonHeadBlock.FACING));
                if (level.getBlockState(headPos).hasProperty(PistonHeadBlock.SHORT)) {
                    BlockPos newHeadPos = pos.relative(newBase.getValue(PistonHeadBlock.FACING));
                    if (level.getBlockState(newHeadPos).getMaterial().isReplaceable()) {

                        level.setBlock(newHeadPos, rotateBlockStateOnAxis(level.getBlockState(headPos), face, ccw), 2);
                        level.setBlock(pos, newBase, 2);
                        level.removeBlock(headPos, false);
                        return Optional.of(face);
                    }
                }
                return Optional.empty();
            }
        }
        if (state.hasProperty(BlockStateProperties.SHORT)) {
            if (state.hasProperty(PistonHeadBlock.FACING)) {
                BlockState newBase = rotateBlockStateOnAxis(state, face, ccw);
                BlockPos headPos = pos.relative(state.getValue(PistonHeadBlock.FACING).getOpposite());
                if (level.getBlockState(headPos).hasProperty(PistonBaseBlock.EXTENDED)) {
                    BlockPos newHeadPos = pos.relative(newBase.getValue(PistonHeadBlock.FACING).getOpposite());
                    if (level.getBlockState(newHeadPos).getMaterial().isReplaceable()) {

                        level.setBlock(newHeadPos, rotateBlockStateOnAxis(level.getBlockState(headPos), face, ccw), 2);
                        level.setBlock(pos, newBase, 2);
                        level.removeBlock(headPos, false);
                        return Optional.of(face);
                    }
                }
                return Optional.empty();
            }
        }
        if (b instanceof BedBlock && face.getAxis() == Direction.Axis.Y) {
            BlockState newBed = state.rotate(level, pos, rot);
            BlockPos oldPos = pos.relative(getConnectedBedDirection(state));
            BlockPos targetPos = pos.relative(getConnectedBedDirection(newBed));
            if (level.getBlockState(targetPos).getMaterial().isReplaceable()) {
                level.setBlock(targetPos, level.getBlockState(oldPos).rotate(level, oldPos, rot), 2);
                level.setBlock(pos, newBed, 2);
                level.removeBlock(oldPos, false);
                return Optional.of(face);
            }
            return Optional.empty();
        }
        if (b instanceof ChestBlock && face.getAxis() == Direction.Axis.Y) {
            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockState newChest = state.rotate(level, pos, rot);
                BlockPos oldPos = pos.relative(ChestBlock.getConnectedDirection(state));
                BlockPos targetPos = pos.relative(ChestBlock.getConnectedDirection(newChest));
                if (level.getBlockState(targetPos).getMaterial().isReplaceable()) {
                    BlockState connectedNewState = level.getBlockState(oldPos).rotate(level, oldPos, rot);
                    level.setBlock(targetPos, connectedNewState, 2);
                    level.setBlock(pos, newChest, 2);

                    BlockEntity tile = level.getBlockEntity(oldPos);
                    if (tile != null) {
                        CompoundTag tag = tile.saveWithoutMetadata();
                        if (level.getBlockEntity(targetPos) instanceof ChestBlockEntity newChestTile) {
                            newChestTile.load(tag);
                        }
                        tile.setRemoved();
                    }

                    level.setBlockAndUpdate(oldPos, Blocks.AIR.defaultBlockState());
                    return Optional.of(face);
                }
            }
            return Optional.empty();
        }
        if (DoorBlock.isWoodenDoor(state)) {
            //TODO: add
            //level.setBlockAndUpdate(state.rotate(level, pos, rot));

        }
        return Optional.empty();
    }

    private static Direction getConnectedBedDirection(BlockState bedState) {
        BedPart part = bedState.getValue(BedBlock.PART);
        Direction dir = bedState.getValue(BedBlock.FACING);
        return part == BedPart.FOOT ? dir : dir.getOpposite();
    }

    //TODO: add rotation vertical slabs & doors

    private static BlockState rotateFaceBlockHorizontal(Direction dir, boolean ccw, BlockState original) {

        Direction facingDir = original.getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (facingDir.getAxis() == dir.getAxis()) return original;

        var face = original.getValue(BlockStateProperties.ATTACH_FACE);
        return switch (face) {
            case FLOOR -> original.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, ccw ? dir.getClockWise() : dir.getCounterClockWise());
            case CEILING -> original.setValue(BlockStateProperties.ATTACH_FACE, AttachFace.WALL)
                    .setValue(BlockStateProperties.HORIZONTAL_FACING, !ccw ? dir.getClockWise() : dir.getCounterClockWise());
            case WALL -> {
                ccw = ccw^(dir.getAxisDirection() != Direction.AxisDirection.POSITIVE);
                yield original.setValue(BlockStateProperties.ATTACH_FACE,
                        (facingDir.getAxisDirection() == Direction.AxisDirection.POSITIVE) ^ ccw ? AttachFace.CEILING : AttachFace.FLOOR);
            }

        };

    }

}
