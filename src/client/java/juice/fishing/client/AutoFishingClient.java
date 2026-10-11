package juice.fishing.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;

public class AutoFishingClient implements ClientModInitializer {

    public static KeyMapping toggleKey;
    public boolean isFishing = false;
    public boolean hasToThrow = true;
    private final int rethrow = 15;
    private int actionCooldown = 0;

    @Override
    public void onInitializeClient() {

        KeyMapping.Category MY_CATEGORY = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath("juice", "autofish")
        );

        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "AutoFish",
                        InputConstants.Type.KEYBOARD,
                        InputConstants.KEY_J,
                        MY_CATEGORY
                )
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                isFishing = !isFishing;
                hasToThrow = isFishing;
                actionCooldown = 0;

                if (client.player != null) {
                    if (isFishing) {
                        client.player.sendSystemMessage(
                                Component.literal("Fishing: ENABLED")
                                        .withStyle(ChatFormatting.GREEN)
                        );
                    } else {
                        client.player.sendSystemMessage(
                                Component.literal("Fishing: DISABLED")
                                        .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            }
            
            if (isFishing && client.player != null) {
                fish(client);
            }
        });
    }

    public void fish(Minecraft client) {
        var player = client.player;
        if (player == null) return;

        if (!player.getMainHandItem().is(Items.FISHING_ROD)) {
            client.player.sendSystemMessage(
                    Component.literal("Pls equip fishing rod\nFishing: DISABLED")
                            .withStyle(ChatFormatting.RED)
            );
            isFishing = false;
            return;
        }

        if (actionCooldown > 0) {
            actionCooldown--;
            return;
        }

        var hook = player.fishing;

        if (hook == null && hasToThrow) {
            client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            hasToThrow = false;
            actionCooldown = rethrow;
            return;
        }

        if (hook == null) return;

        if (areNear(player, hook)) {
            BlockPos hookPos = hook.blockPosition();
            if (!client.level.getBlockState(hookPos).isAir()) {
                client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
                hasToThrow = true;
                actionCooldown = rethrow;
                return;
            }
        }

        double dy = hook.getDeltaMovement().y;
        if (dy < -0.1 && hook.isInWater()) {
            client.gameMode.useItem(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            hasToThrow = true;
            actionCooldown = rethrow;
        }
    }

    private boolean areNear(LocalPlayer p, FishingHook hook) {
        BlockPos playerPos = p.blockPosition();
        BlockPos hookPos = hook.blockPosition();

        return playerPos.distManhattan(hookPos) < 2;
    }
}