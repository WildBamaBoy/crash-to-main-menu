package com.wildbamaboy.crashtomainmenu.gui;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiMinecraftCrashed extends GuiScreen
{
    private final String title;

    public GuiMinecraftCrashed()
    {
        this.title = "Minecraft has crashed!";
    }

    /**
     * Adds the buttons (and other controls) to the screen in question. Called when the GUI is displayed and when the
     * window resizes, the buttonList is cleared beforehand.
     */
    public void initGui()
    {
        super.initGui();
        this.buttonList.add(new GuiButton(0, this.width / 2 - 100, 140, "Close"));
    }

    /**
     * Draws the screen and all the components in it.
     */
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawGradientRect(0, 0, this.width, this.height, -12574688, -11530224);
        this.drawCenteredString(this.fontRenderer, this.title, this.width / 2, 90, 16777215);
        this.drawCenteredString(this.fontRenderer, "A crash report has been saved to your crash-reports folder.", this.width / 2, 110, 16777215);
        this.drawCenteredString(this.fontRenderer, "This crash is recoverable and you will be returned to the main menu.", this.width / 2, 130, 16777215);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    /**
     * Called by the controls from the buttonList when activated. (Mouse pressed for buttons)
     */
    protected void actionPerformed(GuiButton button) throws IOException
    {
        this.mc.displayGuiScreen((GuiScreen)null);
    }
}