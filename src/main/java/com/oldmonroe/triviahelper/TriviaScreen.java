package com.oldmonroe.triviahelper;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/**
 * The converter window.
 *
 * Note the 26.x rendering model: screens override extractRenderState with a
 * GuiGraphicsExtractor rather than the old render(GuiGraphics, ...), and
 * keyPressed takes a KeyEvent instead of loose int arguments.
 */
public class TriviaScreen extends Screen {
	private static final int COLOR_TITLE = 0xFFFFFFFF;
	private static final int COLOR_DIM = 0xFFA0A0A0;
	private static final int COLOR_ANSWER = 0xFF55FF55;
	private static final int COLOR_ERROR = 0xFFFF5555;

	private final String capturedQuestion;

	private EditBox input;
	private String answer = "";
	private String detail = "";
	private boolean lastSolveFailed;
	private String toast = "";
	private long toastUntil;

	public TriviaScreen() {
		super(Component.literal("Trivia Helper"));
		this.capturedQuestion = TriviaCapture.getLastQuestion();
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int top = this.height / 2 - 20;

		this.input = new EditBox(this.font, centerX - 130, top, 260, 20, Component.literal("Input"));
		this.input.setMaxLength(256);

		String seed = TriviaCapture.getLastCandidate();

		if (seed != null && !seed.isEmpty()) {
			this.input.setValue(seed);
		}

		// Live-solve as you type, but do not touch the clipboard until you ask.
		this.input.setResponder(value -> this.recompute());

		this.addRenderableWidget(this.input);
		this.setInitialFocus(this.input);

		this.addRenderableWidget(Button.builder(Component.literal("Solve + Copy"), b -> this.solveAndCopy())
				.pos(centerX - 130, top + 28)
				.size(126, 20)
				.build());

		this.addRenderableWidget(Button.builder(Component.literal("Copy question"), b -> this.copyQuestion())
				.pos(centerX + 4, top + 28)
				.size(126, 20)
				.build());

		this.addRenderableWidget(Button.builder(Component.literal("Close"), b -> this.onClose())
				.pos(centerX - 40, top + 52)
				.size(80, 20)
				.build());

		this.recompute();
	}

	/** Solve without side effects - used for live feedback while typing. */
	private void recompute() {
		TriviaSolver.Result result = TriviaSolver.solve(this.input.getValue());

		if (result.ok()) {
			this.answer = result.answer();
			this.detail = result.explanation();
			this.lastSolveFailed = false;
		} else {
			this.answer = "";
			this.detail = result.explanation();
			this.lastSolveFailed = true;
		}
	}

	private void solveAndCopy() {
		this.recompute();

		if (this.lastSolveFailed || this.answer.isEmpty()) {
			this.showToast("Nothing to copy");
			return;
		}

		TriviaCapture.setClipboard(this.answer);
		this.showToast("Copied \"" + this.answer + "\" to clipboard");
	}

	private void copyQuestion() {
		if (this.capturedQuestion == null || this.capturedQuestion.isEmpty()) {
			this.showToast("No question captured yet");
			return;
		}

		TriviaCapture.setClipboard(this.capturedQuestion);
		this.showToast("Copied the question");
	}

	private void showToast(String text) {
		this.toast = text;
		this.toastUntil = System.currentTimeMillis() + 2500L;
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();

		if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
			this.solveAndCopy();
			return true;
		}

		return super.keyPressed(event);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		super.extractRenderState(graphics, mouseX, mouseY, delta);

		int centerX = this.width / 2;
		int top = this.height / 2 - 20;

		graphics.centeredText(this.font, "Trivia Helper", centerX, top - 44, COLOR_TITLE);

		if (this.capturedQuestion != null && !this.capturedQuestion.isEmpty()) {
			graphics.centeredText(this.font, truncate(this.capturedQuestion, 58), centerX, top - 28, COLOR_DIM);
		} else {
			graphics.centeredText(this.font, "No question captured yet", centerX, top - 28, COLOR_DIM);
		}

		if (!this.answer.isEmpty()) {
			graphics.centeredText(this.font, "= " + this.answer, centerX, top + 80, COLOR_ANSWER);
		}

		if (!this.detail.isEmpty()) {
			int color = this.lastSolveFailed ? COLOR_ERROR : COLOR_DIM;
			graphics.centeredText(this.font, truncate(this.detail, 58), centerX, top + 94, color);
		}

		if (!this.toast.isEmpty() && System.currentTimeMillis() < this.toastUntil) {
			graphics.centeredText(this.font, this.toast, centerX, top + 112, COLOR_TITLE);
		}
	}

	private static String truncate(String text, int max) {
		if (text.length() <= max) {
			return text;
		}

		return text.substring(0, max - 3) + "...";
	}
}
