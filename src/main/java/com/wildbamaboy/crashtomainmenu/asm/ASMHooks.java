package com.wildbamaboy.crashtomainmenu.asm;

import com.wildbamaboy.crashtomainmenu.MinecraftOverrides;
import com.wildbamaboy.crashtomainmenu.gui.GuiMinecraftCrashed;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiErrorScreen;
import net.minecraft.client.gui.GuiMemoryErrorScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.MinecraftError;
import net.minecraft.util.ReportedException;
import net.minecraftforge.fml.common.FMLCommonHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;

import static net.minecraft.client.Minecraft.getMinecraft;

public class ASMHooks {
    private static final Minecraft minecraft = Minecraft.getMinecraft();

    /**
     * Call inserted into the top of Minecraft.run() through ASM.
     */
    public static void onMinecraftRun() {
        final Logger LOGGER = LogManager.getLogger();
        LOGGER.warn("CrashToMainMenu is loaded properly and now launching Minecraft...");

        /**
         * What follows is a form of the original Minecraft.run() call but executed here instead so we have control
         * over which exceptions we allow to crash the game and which we can recover from.
         */
        MinecraftOverrides.setMinecraftField(MinecraftOverrides.RUNNING, true);

        try {
            MinecraftOverrides.INIT.invoke(minecraft);
        } catch (Throwable throwable) {
            // Crashes on init aren't salvageable. Allow the crash in this case.
            CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Initializing game");
            crashreport.makeCategory("Initialization");
            minecraft.displayCrashReport(minecraft.addGraphicsAndWorldToCrashReport(crashreport));
            return;
        }

        // Initialization finished, begin main game loop.
        while (true) {
            try {
                /* Run the game loop wrapped in this function. Since we call runGameLoop() through reflection
                 * we'll get an InvocationTargetException back wrapped around the initial cause. This
                 * will unwrap the InvocationTargetException to the original exception so we can respect original
                 * handling of crashes through the catch blocks below. */
                while ((Boolean)MinecraftOverrides.RUNNING.get(minecraft)) {
                    if (!(Boolean)MinecraftOverrides.HAS_CRASHED.get(minecraft)){
                        try {
                            MinecraftOverrides.RUN_GAME_LOOP.invoke(minecraft);
                        } catch (InvocationTargetException e) {
                            throw e.getCause(); // pass the cause out to the run loop
                        }
                    } else { // Minecraft crashed, forget about it!
                        MinecraftOverrides.setMinecraftField(MinecraftOverrides.HAS_CRASHED, false);
                    }
                }
            } catch (OutOfMemoryError e) {
                minecraft.freeMemory();
                minecraft.displayGuiScreen(new GuiMemoryErrorScreen());
                System.gc();
            } catch (MinecraftError var12) {
                break; //No idea what this would be for...
            } catch (ReportedException reportedexception) {
                minecraft.addGraphicsAndWorldToCrashReport(reportedexception.getCrashReport());
                minecraft.freeMemory();
                LOGGER.fatal("Reported exception thrown!", (Throwable)reportedexception);
                MinecraftOverrides.displayCrashReport(reportedexception.getCrashReport());
                MinecraftOverrides.resetMinecraft();
                getMinecraft().displayGuiScreen(new GuiMinecraftCrashed());
                continue;
            } catch (Throwable throwable1) {
                CrashReport crashreport1 = minecraft.addGraphicsAndWorldToCrashReport(new CrashReport("Unexpected error", throwable1));
                minecraft.freeMemory();
                LOGGER.fatal("Unreported exception thrown!", throwable1);
                MinecraftOverrides.displayCrashReport(crashreport1);
                MinecraftOverrides.resetMinecraft();
                getMinecraft().displayGuiScreen(new GuiMinecraftCrashed());
                continue;
            } finally {
                // Allow clean exit from the main menu if Minecraft cleanly shut down.
                try {
                    if (!(Boolean) MinecraftOverrides.RUNNING.get(minecraft)) {
                        // FML doesn't allow us to call exit from Minecraft or system exit, but it does provide
                        // an exit function which is always allowed to run regardless of the calling parent.
                        FMLCommonHandler.instance().exitJava(0, false);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return;
        }
    }
}
