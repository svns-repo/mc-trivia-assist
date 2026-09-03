package com.oldmonroe.triviahelper;

import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Watches incoming chat for trivia questions, remembers the most recent one,
 * and puts it on the system clipboard.
 *
 * All state is static because there is only ever one client.
 */
public final class TriviaCapture {
	private TriviaCapture() {
	}

	/**
	 * Matches the trivia mod's prefix, e.g. "| ! | What number is ...".
	 * The message is stripped of colour codes before matching, so this only
	 * has to deal with plain text. Edit this if your trivia mod's format
	 * differs - everything else keys off the captured group.
	 */
	private static final Pattern TRIVIA_PREFIX = Pattern.compile("^\\s*\\|\\s*!\\s*\\|\\s*(.+?)\\s*$");

	/**
	 * Set to true to also capture any chat line ending in a question mark.
	 * Off by default so ordinary chat does not clobber your clipboard.
	 */
	private static final boolean CAPTURE_ANY_QUESTION = false;

	/** Set to false if you would rather the clipboard was left alone. */
	private static final boolean AUTO_COPY_QUESTION = true;

	private static volatile String lastQuestion = "";
	private static volatile String lastCandidate = "";

	public static void onMessage(Component message) {
		if (message == null) {
			return;
		}

		// getString() drops formatting codes and gives us plain text.
		String plain = message.getString();

		if (plain == null || plain.isBlank()) {
			return;
		}

		String question = null;
		var matcher = TRIVIA_PREFIX.matcher(plain);

		if (matcher.matches()) {
			question = matcher.group(1);
		} else if (CAPTURE_ANY_QUESTION && plain.trim().endsWith("?")) {
			question = plain.trim();
		}

		if (question == null) {
			return;
		}

		lastQuestion = question;

		String candidate = TriviaSolver.extractCandidate(question);
		lastCandidate = candidate == null ? "" : candidate;

		if (AUTO_COPY_QUESTION) {
			setClipboard(question);
		}
	}

	public static String getLastQuestion() {
		return lastQuestion;
	}

	/**
	 * The bit of the question worth solving - the Roman numeral or expression
	 * pulled out of the sentence. Used to pre-fill the input box.
	 */
	public static String getLastCandidate() {
		return lastCandidate;
	}

	public static void setClipboard(String text) {
		if (text == null || text.isEmpty()) {
			return;
		}

		Minecraft client = Minecraft.getInstance();

		if (client == null) {
			return;
		}

		try {
			client.keyboardHandler.setClipboard(text);
		} catch (Exception e) {
			TriviaHelperClient.LOGGER.warn("Could not write to clipboard", e);
		}
	}
}
