# Trivia Helper

A client-side Fabric mod for Minecraft 26.2. It watches chat for trivia
questions, copies them to your clipboard, and gives you a hotkey-opened window
that converts Roman numerals, converts numbers back to Roman numerals, and
evaluates basic arithmetic — copying the answer out so you can paste it.

## What it does

- **Chat capture.** Any chat line matching `| ! | <question>` is remembered and
  copied to the clipboard automatically.
- **Hotkey.** Default bind is **mouse Button 4** (the near side button). Rebind
  it under Options → Controls → Miscellaneous, "Open Trivia Helper".
- **The window.** The input box is pre-filled with whatever was worth solving in
  the last captured question — the Roman numeral, or the expression. It solves
  live as you type. Enter, or the **Solve + Copy** button, puts the answer on
  your clipboard.

It handles all three directions:

| You type | You get |
| --- | --- |
| `MMDCCXXXVI` | `2736` |
| `2736` | `MMDCCXXXVI` |
| `17 * 3 + 4` | `55` |

The math evaluator supports `+ - * / % ^`, parentheses, unary minus, and
decimals, with correct precedence. It uses `BigDecimal`, so large values and
division do not lose precision the way `double` would.

If you paste in a whole question sentence, it pulls the numeral or expression
out of it rather than giving up.

## Building

Requires **JDK 25** — 26.1 was the first Minecraft release to need it.

```
./gradlew build
```

The jar lands in `build/libs/triviahelper-1.0.0.jar`. Ignore the `-sources`
one.

The versions in `gradle.properties` were current as of the 26.2 release; check
https://fabricmc.net/develop if the build fails on a dependency resolution
error, and bump them there.

## Installing

**Plain Fabric:** drop the jar plus Fabric API into `.minecraft/mods`.

**Lunar Client:** open the version selector, pick 26.2, enable the Fabric
add-on, click the gear button, and drag the jar into the Mods tab. Lunar
bundles Fabric API itself.

Test on plain Fabric first. Lunar's mod loading is a layer on top of their own
client and they are open about not every Fabric mod working — if something
misbehaves, you want to already know whether it is the mod or the client.

## Adapting it to your trivia mod

Everything format-specific lives at the top of `TriviaCapture.java`:

- `TRIVIA_PREFIX` — the regex for your trivia mod's chat format. The message is
  stripped of colour codes before matching, so you only need to match plain
  text. Group 1 is the question.
- `CAPTURE_ANY_QUESTION` — set true to also grab any line ending in `?`. Off by
  default so ordinary chat does not overwrite your clipboard.
- `AUTO_COPY_QUESTION` — set false to leave the clipboard alone until you press
  a button.

If your trivia mod posts questions as action-bar/overlay text rather than real
chat lines, the `GAME` listener in `TriviaHelperClient` currently skips those
(`if (!overlay)`); drop that check.

## A note on 26.2

This targets the unobfuscated era, which is a real break from 1.21.x tutorials:

- No `mappings` line and no Yarn — Mojang names only.
- The Loom plugin id is `net.fabricmc.fabric-loom`, not `fabric-loom`.
- `modImplementation` is now plain `implementation`.
- Rendering: `GuiGraphics` → `GuiGraphicsExtractor`, and screens override
  `extractRenderState` rather than `render`.
- `Screen.keyPressed` takes a `KeyEvent`, not loose ints.
- Keybinds come from `KeyMappingHelper` in `...api.client.keymapping.v1`, and
  categories are `KeyMapping.Category` objects rather than strings.

The solver logic in `TriviaSolver.java` has no Minecraft imports, so you can
compile and test it standalone with any JDK.
