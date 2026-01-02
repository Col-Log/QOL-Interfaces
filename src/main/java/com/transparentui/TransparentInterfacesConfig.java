package com.transparentui;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("transparentinterfaces")
public interface TransparentInterfacesConfig extends Config
{
    @ConfigItem(
        keyName = "hideGEBackground",
        name = "Hide GE Background",
        description = "Toggle the Grand Exchange background transparent",
        position = 1
    )
    default boolean hideGEBackground()
    {
        return false;
    }

    @ConfigItem(
        keyName = "hideBankBackground",
        name = "Hide Bank Background",
        description = "Toggle the Bank background transparent",
        position = 2
    )
    default boolean hideBankBackground()
    {
        return false;
    }

    @ConfigItem(
        keyName = "moveableGE",
        name = "Moveable GE",
        description = "Allow dragging the Grand Exchange interface to reposition it",
        position = 3
    )
    default boolean moveableGE()
    {
        return false;
    }

    @ConfigItem(
        keyName = "moveableBank",
        name = "Moveable Bank",
        description = "Allow dragging the Bank interface to reposition it",
        position = 4
    )
    default boolean moveableBank()
    {
        return false;
    }

    @ConfigItem(
        keyName = "tradeFriendWarning",
        name = "Trade Friend Warning",
        description = "Show if trade partner is on your friends list",
        position = 5
    )
    default boolean tradeFriendWarning()
    {
        return true;
    }
}
