package com.oldmonroe.triviahelper;

import com.mojang.blaze3d.platform.InputConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.KeyMapping;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

public class TriviaHelperClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("triviahelper");

	private static KeyMapping openKey;

	@Override
	public void onInitializeClient() {
		// Default bind is mouse Button 4 (the near side button on most mice).
		// Rebind it in Options > Controls > Miscellaneous if that clashes.
		openKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.triviahelper.open",
				InputConstants.Type.MOUSE,
				3,
				KeyMapping.Category.MISC));

		// GAME fires for server/system messages, which is what a singleplayer
		// mod posting to chat almost certainly uses. CHAT fires for messages
		// attributed to a player. Registering both costs nothing.
		ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
			if (!overlay) {
				TriviaCapture.onMessage(message);
			}
		});

		ClientReceiveMessageEvents.CHAT.register((message, signed, sender, boundChatType, timestamp) ->
				TriviaCapture.onMessage(message));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				if (client.gui.screen() == null) {
  				  client.gui.setScreen(new TriviaScreen());
				}
			}
		});

		LOGGER.info("Trivia Helper ready");
	}
}
