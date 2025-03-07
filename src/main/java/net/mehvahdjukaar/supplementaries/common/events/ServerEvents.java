package net.mehvahdjukaar.supplementaries.common.events;


import net.mehvahdjukaar.supplementaries.Supplementaries;
import net.mehvahdjukaar.supplementaries.common.block.blocks.RakedGravelBlock;
import net.mehvahdjukaar.supplementaries.common.capabilities.CapabilityHandler;
import net.mehvahdjukaar.supplementaries.common.configs.RegistryConfigs;
import net.mehvahdjukaar.supplementaries.common.configs.ServerConfigs;
import net.mehvahdjukaar.supplementaries.common.entities.goals.EatFodderGoal;
import net.mehvahdjukaar.supplementaries.common.items.CandyItem;
import net.mehvahdjukaar.supplementaries.common.network.ClientBoundSendLoginPacket;
import net.mehvahdjukaar.supplementaries.common.network.ClientBoundSyncAntiqueInk;
import net.mehvahdjukaar.supplementaries.common.network.NetworkHandler;
import net.mehvahdjukaar.supplementaries.common.utils.ModTags;
import net.mehvahdjukaar.supplementaries.common.world.data.GlobeData;
import net.mehvahdjukaar.supplementaries.common.world.songs.FluteSongsReloadListener;
import net.mehvahdjukaar.supplementaries.common.world.songs.SongsManager;
import net.mehvahdjukaar.supplementaries.setup.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.UseHoeEvent;
import net.minecraftforge.event.world.ChunkWatchEvent;
import net.minecraftforge.event.world.NoteBlockEvent;
import net.minecraftforge.event.world.PistonEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Set;


@Mod.EventBusSubscriber(modid = Supplementaries.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {

    //high priority event to override other wall lanterns
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlockHigh(PlayerInteractEvent.RightClickBlock event) {
        if (ServerConfigs.cached.WALL_LANTERN_HIGH_PRIORITY) {
            Player player = event.getPlayer();
            if (!player.isSpectator()) {
                ItemsOverrideHandler.tryHighPriorityClickedBlockOverride(event, event.getItemStack());
            }
        }
    }

    //block placement should stay low in priority to allow other more important mod interaction that use the event
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getPlayer();
        if (!player.isSpectator()) {
            ItemStack stack = event.getItemStack();

            ItemsOverrideHandler.tryPerformClickedBlockOverride(event, stack, false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player playerIn = event.getPlayer();

        ItemsOverrideHandler.tryPerformClickedItemOverride(event, playerIn.getItemInHand(event.getHand()));
    }

    //raked gravel
    @SubscribeEvent
    public static void onHoeUsed(UseHoeEvent event) {
        UseOnContext context = event.getContext();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (ServerConfigs.cached.RAKED_GRAVEL) {
            if (world.getBlockState(pos).is(Blocks.GRAVEL)) {
                BlockState raked = ModRegistry.RAKED_GRAVEL.get().defaultBlockState();
                if (raked.canSurvive(world, pos)) {
                    world.setBlock(pos, RakedGravelBlock.getConnectedState(raked, world, pos, context.getHorizontalDirection()), 11);
                    world.playSound(context.getPlayer(), pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    event.setResult(Event.Result.ALLOW);
                }
            }
        }
    }


    @SubscribeEvent
    public static void onAttachTileCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        CapabilityHandler.attachCapabilities(event);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side == LogicalSide.SERVER) {
            CandyItem.checkSweetTooth(event.player);
        }
    }

    private static final boolean FODDER_ENABLED = RegistryConfigs.reg.FODDER_ENABLED.get();

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinWorldEvent event) {
        if (FODDER_ENABLED) {
            Entity entity = event.getEntity();
            if (entity instanceof Animal animal) {
                EntityType<?> type = event.getEntity().getType();
                if (ModTags.EATS_FODDER.contains(type)) {
                    animal.goalSelector.addGoal(3,
                            new EatFodderGoal(animal, 1, 8, 2, 30));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        try {
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getPlayer()),
                    new ClientBoundSendLoginPacket(UsernameCache.getMap()));
        } catch (Exception exception) {
            Supplementaries.LOGGER.warn("failed to end login message: " + exception);
        }
        GlobeData.sendGlobeData(event);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(final AddReloadListenerEvent event) {
        event.addListener(new FluteSongsReloadListener());
    }

    @SubscribeEvent
    public static void onPistonMoved(final PistonEvent.Post event) {

        if (event.getPistonMoveType() == PistonEvent.PistonMoveType.RETRACT) {
            LevelAccessor level = event.getWorld();
            var pos = event.getPos();
        }
    }

    @SubscribeEvent
    public static void noteBlockEvent(final NoteBlockEvent.Play event) {
        SongsManager.recordNote(event.getWorld(), event.getPos());
    }

    //TODO: remove when forge PR gets approved. using mixin right now
    //antique ink sync
    //@SubscribeEvent
    public static void onPlayerStartTracking(final ChunkWatchEvent.Watch event) {
        ServerLevel serverLevel = event.getWorld();

        //TODO: this is slow af. mixin in ChunkHolder
        ChunkAccess chunk = serverLevel.getChunkSource().getChunk(event.getPos().x, event.getPos().z, ChunkStatus.FULL, false);
        if (chunk != null) {
            Set<BlockPos> positions = chunk.getBlockEntitiesPos();
            LevelAccessor level = event.getWorld();
            for (BlockPos pos : positions) {
                BlockEntity te = level.getBlockEntity(pos);
                if (te != null) {
                    var cap = te.getCapability(CapabilityHandler.ANTIQUE_TEXT_CAP);
                    if (cap.isPresent()) {
                        MinecraftServer server = serverLevel.getServer();
                        var c = cap.orElse(null);
                        boolean a = c.hasAntiqueInk();
                        server.tell(new TickTask(server.getTickCount(), () ->
                                NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(event::getPlayer),
                                        new ClientBoundSyncAntiqueInk(pos, a))));
                    }
                }
            }
        }
    }


}
