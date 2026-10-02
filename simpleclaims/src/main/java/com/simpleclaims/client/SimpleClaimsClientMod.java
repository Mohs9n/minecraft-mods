package com.simpleclaims.client;

import com.simpleclaims.network.OpenClaimNameScreenPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

public class SimpleClaimsClientMod implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // The receiver fires on the network thread - screen changes must happen on the
        // render thread, so hop over via client.execute().
        ClientPlayNetworking.registerGlobalReceiver(OpenClaimNameScreenPayload.TYPE, (payload, context) -> {
            Minecraft client = context.client();
            client.execute(() -> client.setScreenAndShow(new ClaimNameScreen()));
        });
    }
}
