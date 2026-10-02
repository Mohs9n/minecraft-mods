package com.simpleclaims.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Server -> client: "open the custom claim-naming screen now." Carries no data; the
 * client already knows (from its own GUI click) that it just asked for this.
 */
public record OpenClaimNameScreenPayload() implements CustomPacketPayload {
    public static final Type<OpenClaimNameScreenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("simpleclaims", "open_claim_name_screen"));

    public static final StreamCodec<ByteBuf, OpenClaimNameScreenPayload> CODEC =
            StreamCodec.unit(new OpenClaimNameScreenPayload());

    @Override
    public Type<OpenClaimNameScreenPayload> type() {
        return TYPE;
    }
}
