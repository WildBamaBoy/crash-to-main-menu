# Crash To Main Menu
Allows Minecraft to crash to its main menu instead of to the desktop.

## How It Works
This is a coremod that re-implements `Minecraft.run()`. A hook into `Minecraft.run()` is injected with ASM that directs execution to our version instead. Our version of `Minecraft.run()` is functionally identical except that we don't exit the game, we simply show an error message and return to the main menu.

Saving crash reports is preserved and functions as normal.

Crashes that occur outside of a loaded world still cause a normal crash-to-desktop.

## Credits
Vazkii's Quark source code, which was used as a base for applying the needed ASM transformation.
