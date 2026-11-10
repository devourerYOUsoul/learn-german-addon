package com.deutschimspiel;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The two small network messages of the mod. */
public final class Net {
    private Net() {}

    /** Client -> server: "apply this penalty to me". Parameters travel along because the server has no client config. */
    public record PunishRequest(String key, int creepers, int villagers, int pumpkinSeconds,
                                int poisonSeconds, int damageHp, int launchMinHealth,
                                boolean wholeStack) implements CustomPacketPayload {
        public static final Type<PunishRequest> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(DeutschImSpiel.MODID, "punish"));
        public static final StreamCodec<FriendlyByteBuf, PunishRequest> CODEC = StreamCodec.of(
                (buf, v) -> {
                    buf.writeUtf(v.key, 32);
                    buf.writeVarInt(v.creepers);
                    buf.writeVarInt(v.villagers);
                    buf.writeVarInt(v.pumpkinSeconds);
                    buf.writeVarInt(v.poisonSeconds);
                    buf.writeVarInt(v.damageHp);
                    buf.writeVarInt(v.launchMinHealth);
                    buf.writeBoolean(v.wholeStack);
                },
                buf -> new PunishRequest(buf.readUtf(32), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: "give me this reward". */
    public record RewardRequest(String key) implements CustomPacketPayload {
        public static final Type<RewardRequest> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(DeutschImSpiel.MODID, "reward"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RewardRequest> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, RewardRequest::key, RewardRequest::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client -> server: a window is open, so the player must not take damage. */
    public record PromptState(boolean open) implements CustomPacketPayload {
        public static final Type<PromptState> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(DeutschImSpiel.MODID, "prompt_state"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PromptState> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, PromptState::open, PromptState::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server -> client: "you just caught this item" (fishing happens on the server). */
    public record FishNotice(String itemId) implements CustomPacketPayload {
        public static final Type<FishNotice> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(DeutschImSpiel.MODID, "fish"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FishNotice> CODEC =
                StreamCodec.composite(ByteBufCodecs.STRING_UTF8, FishNotice::itemId, FishNotice::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(PunishRequest.TYPE, PunishRequest.CODEC, ServerPunish::onRequest);
        r.playToServer(RewardRequest.TYPE, RewardRequest.CODEC, ServerRewards::onRequest);
        r.playToServer(PromptState.TYPE, PromptState.CODEC, ServerPunish::onPromptState);
        IPayloadHandler<FishNotice> fish = FMLEnvironment.dist == Dist.CLIENT
                ? ClientHandlers::onFish
                : (payload, ctx) -> {};
        r.playToClient(FishNotice.TYPE, FishNotice.CODEC, fish);
    }
}
