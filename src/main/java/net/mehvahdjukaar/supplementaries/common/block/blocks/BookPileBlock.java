package net.mehvahdjukaar.supplementaries.common.block.blocks;

import net.mehvahdjukaar.selene.blocks.WaterBlock;
import net.mehvahdjukaar.supplementaries.common.block.BlockProperties;
import net.mehvahdjukaar.supplementaries.common.block.tiles.BookPileBlockTile;
import net.mehvahdjukaar.supplementaries.common.configs.ServerConfigs;
import net.mehvahdjukaar.supplementaries.common.utils.ModTags;
import net.mehvahdjukaar.supplementaries.integration.CompatHandler;
import net.mehvahdjukaar.supplementaries.integration.quark.QuarkPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.List;

public class BookPileBlock extends WaterBlock implements EntityBlock {

    private static final VoxelShape SHAPE_1 = Block.box(3D, 0D, 3D, 13D, 4D, 13D);
    private static final VoxelShape SHAPE_2 = Block.box(3D, 0D, 3D, 13D, 8D, 13D);
    private static final VoxelShape SHAPE_3 = Block.box(3D, 0D, 3D, 13D, 12D, 13D);
    private static final VoxelShape SHAPE_4 = Block.box(3D, 0D, 3D, 13D, 16D, 13D);

    public static final IntegerProperty BOOKS = BlockProperties.BOOKS;

    public BookPileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false).setValue(BOOKS, 1));
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        if (world.getBlockEntity(pos) instanceof BookPileBlockTile tile) {
            ItemStack copy = stack.copy();
            copy.setCount(1);
            tile.setItem(state.getValue(BOOKS) - 1, copy);
        }
    }

    public boolean isAcceptedItem(Item i) {
        return isEnchantedBook(i) || (ServerConfigs.cached.MIXED_BOOKS && isNormalBook(i));
    }

    public static boolean isEnchantedBook(Item i) {
        return i == Items.ENCHANTED_BOOK || isQuarkTome(i);
    }

    public static boolean isNormalBook(Item i) {
        return ModTags.BOOKS.contains(i) || (ServerConfigs.cached.WRITTEN_BOOKS && isWrittenBook(i));
    }

    public static boolean isWrittenBook(Item i) {
        return i instanceof WrittenBookItem || i instanceof WritableBookItem;
    }

    public static boolean isQuarkTome(Item i) {
        return CompatHandler.quark && QuarkPlugin.isTome(i);
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (state.getValue(BOOKS) < 4) {
            Item item = context.getItemInHand().getItem();
            if (isAcceptedItem(item)) {
                return true;
            }
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BOOKS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState blockstate = context.getLevel().getBlockState(context.getClickedPos());
        if (blockstate.getBlock() instanceof BookPileBlock) {
            return blockstate.setValue(BOOKS, blockstate.getValue(BOOKS) + 1);
        }
        return super.getStateForPlacement(context);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new BookPileBlockTile(pPos, pState, false);
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            if (world.getBlockEntity(pos) instanceof BookPileBlockTile tile) {
                Containers.dropContents(world, pos, tile);
                world.updateNeighbourForOutputSignal(pos, this);
            }
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
        if (world.getBlockEntity(pos) instanceof BookPileBlockTile tile) {
            double f = 5 * (target.getLocation().y - pos.getY()) / SHAPE_4.bounds().maxY;
            return tile.getItem(Mth.clamp((int) f, 0, state.getValue(BOOKS)));
        }
        return Items.BOOK.getDefaultInstance();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(BOOKS)) {
            default -> SHAPE_1;
            case 2 -> SHAPE_2;
            case 3 -> SHAPE_3;
            case 4 -> SHAPE_4;
        };
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof BookPileBlockTile tile) {
            return tile.getEnchantPower();
        }
        return 0;
    }
}
