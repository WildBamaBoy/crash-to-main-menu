package com.wildbamaboy.crashtomainmenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.crash.CrashReport;
import net.minecraft.init.Bootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

import static net.minecraft.client.Minecraft.getMinecraft;

public class MinecraftOverrides {
    private static final Minecraft minecraft = Minecraft.getMinecraft();
    public static final Field RUNNING;
    public static final Field HAS_CRASHED;
    public static final Field DEBUG_CRASH_KEY_PRESS_TIME;
    public static final Method INIT;
    public static final Method RUN_GAME_LOOP;
    private static final Map<String, String> DEOBF_TO_SRG_MAP = new HashMap<>();
    private static final Logger log = LogManager.getLogger("CrashToMainMenu");

    static {
        DEOBF_TO_SRG_MAP.put("init", "func_71384_a");
        DEOBF_TO_SRG_MAP.put("runGameLoop", "func_71411_J");
        DEOBF_TO_SRG_MAP.put("running", "field_71425_J");
        DEOBF_TO_SRG_MAP.put("hasCrashed", "field_71434_R");
        DEOBF_TO_SRG_MAP.put("debugCrashKeyPressTime", "field_83002_am");

        RUNNING = getMinecraftField("running");
        HAS_CRASHED = getMinecraftField("hasCrashed");
        DEBUG_CRASH_KEY_PRESS_TIME = getMinecraftField("debugCrashKeyPressTime");
        INIT = getMinecraftMethod("init");
        RUN_GAME_LOOP = getMinecraftMethod("runGameLoop");
    }

    /**
     * Resets the game after a crash.
     */
    public static void resetMinecraft() {
        try {
            DEBUG_CRASH_KEY_PRESS_TIME.set(minecraft, -1L);
            RUNNING.set(minecraft, true);
            HAS_CRASHED.set(minecraft, false);
        } catch (IllegalAccessException e) {
            log.fatal("IllegalAccessException while resetting Minecraft!", e);
        }
    }

    /**
     * Our custom implementation of displayCrashReport. This one doesn't report to FML which causes the game to close.
     * But it will still write the crash report out to file.
     */
    public static void displayCrashReport(CrashReport crashReportIn)
    {
        File file1 = new File(getMinecraft().mcDataDir, "crash-reports");
        File file2 = new File(file1, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-client.txt");
        Bootstrap.printToSYSOUT(crashReportIn.getCompleteReport());
        if (crashReportIn.getFile() != null) {
            Bootstrap.printToSYSOUT("#@!@# Game crashed! Crash report saved to: #@!@# " + crashReportIn.getFile());
        }
        else if (crashReportIn.saveToFile(file2)) {
            Bootstrap.printToSYSOUT("#@!@# Game crashed! Crash report saved to: #@!@# " + file2.getAbsolutePath());
        }
        else {
            Bootstrap.printToSYSOUT("#@?@# Game crashed! Crash report could not be saved. #@?@#");
        }
    }

    private static Field getMinecraftField(String name) {
        String obfName = DEOBF_TO_SRG_MAP.getOrDefault(name, "");
        log.info("Looking up field [deobf => obf]: " + name + " => " + obfName);
        try {
            Field deobf = Minecraft.class.getDeclaredField(name);
            deobf.setAccessible(true);
            return deobf;
        } catch (NoSuchFieldException e) {
            try {
                Field obf = Minecraft.class.getDeclaredField(DEOBF_TO_SRG_MAP.getOrDefault(name, ""));
                obf.setAccessible(true);
                return obf;
            }
            catch (NoSuchFieldException e1) {
                log.fatal("Failed to find field!", e1);
            }
        }

        return null;
    }

    private static Method getMinecraftMethod(String name) {
        String obfName = DEOBF_TO_SRG_MAP.getOrDefault(name, "");
        log.info("Looking up method [deobf => obf]: " + name + " => " + obfName);
        try {
            Method deobf = Minecraft.class.getDeclaredMethod(name);
            deobf.setAccessible(true);
            return deobf;
        } catch (NoSuchMethodException e) {
            try {
                Method obf = Minecraft.class.getDeclaredMethod(DEOBF_TO_SRG_MAP.getOrDefault(name, ""));
                obf.setAccessible(true);
                return obf;
            }
            catch (NoSuchMethodException e1) {
                log.fatal("Failed to find method!", e1);
            }
        }
        return null;
    }

    public static void setMinecraftField(Field f, Object value) {
        try {
            f.set(minecraft, value);
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException when setting Minecraft field!", e);
        }
    }
}
