package com.transparentui;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class TransparentInterfacesPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(TransparentInterfacesPlugin.class);
        RuneLite.main(args);
    }
}
