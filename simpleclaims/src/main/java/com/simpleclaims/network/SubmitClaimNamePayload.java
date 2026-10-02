package com.simpleclaims.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: the name the player typed and confirmed in the custom claim-naming
 * screen. The server re-validates everything (selection exists, name format, overlap,
 * claim limits) - the client's own validation is purely a UX nicety, never trusted alone.
 */
public record SubmitClaimNamePayload(String name) implements CustomPacketPayload {
    public static final Type<SubmitClaimNamePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("simpleclaims", "submit_claim_name"));

    public static final StreamCodec<ByteBuf, SubmitClaimNamePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32), SubmitClaimNamePayload::name,
            SubmitClaimNamePayload::new
    );

    @Override
    public Type<SubmitClaimNamePayload> type() {
        return TYPE;
    }
}
