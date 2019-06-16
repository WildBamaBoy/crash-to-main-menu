package com.wildbamaboy.crashtomainmenu.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.DependsOn("forge")
@IFMLLoadingPlugin.TransformerExclusions({ "com.wildbamaboy.crashtomainmenu.asm" })
public class LoadingPlugin implements IFMLLoadingPlugin {
    public static boolean runtimeDeobfEnabled = false;

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{ ClassTransformer.class.getName() };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        runtimeDeobfEnabled = (Boolean) data.get("runtimeDeobfuscationEnabled");
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}