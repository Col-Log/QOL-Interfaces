package com.transparentui;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.BeforeRender;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "QOL Interfaces",
    description = "Quality of life improvements for game interfaces - transparency, repositioning, and more",
    tags = {"interface", "transparency", "ui", "grand exchange", "bank", "qol", "moveable"}
)
public class TransparentInterfacesPlugin extends Plugin
{
    // Grand Exchange: 465.2[0]
    private static final int GE_INTERFACE_ID = InterfaceID.GRAND_EXCHANGE;
    private static final int GE_BACKGROUND_CHILD_ID = 2;

    // Bank: 12.2[0]
    private static final int BANK_INTERFACE_ID = InterfaceID.BANK;
    private static final int BANK_BACKGROUND_CHILD_ID = 2;

    @Inject
    private Client client;

    @Inject
    private TransparentInterfacesConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MouseManager mouseManager;

    @Inject
    private GrandExchangeOverlay geOverlay;

    @Inject
    private BankOverlay bankOverlay;

    @Inject
    private TradeOverlay tradeOverlay;

    @Override
    protected void startUp() throws Exception
    {
        log.debug("Transparent Interfaces started!");
        overlayManager.add(geOverlay);
        overlayManager.add(bankOverlay);
        overlayManager.add(tradeOverlay);
        mouseManager.registerMouseListener(geOverlay);
        mouseManager.registerMouseListener(bankOverlay);
        applyGETransparency();
        applyBankTransparency();
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug("Transparent Interfaces stopped!");
        mouseManager.unregisterMouseListener(geOverlay);
        mouseManager.unregisterMouseListener(bankOverlay);
        overlayManager.remove(geOverlay);
        overlayManager.remove(bankOverlay);
        overlayManager.remove(tradeOverlay);
        geOverlay.resetPosition();
        bankOverlay.resetPosition();
        resetGETransparency();
        resetBankTransparency();
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        geOverlay.onGameTick();
        bankOverlay.onGameTick();
    }

    @Subscribe
    public void onBeforeRender(BeforeRender event)
    {
        // Apply position right before each frame renders to prevent flicker
        geOverlay.onBeforeRender();
        bankOverlay.onBeforeRender();
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (event.getGroupId() == GE_INTERFACE_ID)
        {
            applyGETransparency();
        }
        else if (event.getGroupId() == BANK_INTERFACE_ID)
        {
            applyBankTransparency();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals("transparentinterfaces"))
        {
            applyGETransparency();
            applyBankTransparency();
        }
    }

    private void applyGETransparency()
    {
        Widget geFrame = client.getWidget(GE_INTERFACE_ID, GE_BACKGROUND_CHILD_ID);

        if (geFrame == null)
        {
            return;
        }

        // Get the first sub-child (465.2[0]) which is the actual background
        Widget[] children = geFrame.getDynamicChildren();
        if (children == null || children.length == 0)
        {
            return;
        }

        Widget geBackground = children[0];
        if (geBackground != null)
        {
            // 200 = transparent, 0 = opaque
            int opacity = config.hideGEBackground() ? 200 : 0;
            geBackground.setOpacity(opacity);
            log.debug("Applied GE background opacity: {}", opacity);
        }
    }

    private void resetGETransparency()
    {
        Widget geFrame = client.getWidget(GE_INTERFACE_ID, GE_BACKGROUND_CHILD_ID);

        if (geFrame == null)
        {
            return;
        }

        Widget[] children = geFrame.getDynamicChildren();
        if (children == null || children.length == 0)
        {
            return;
        }

        Widget geBackground = children[0];
        if (geBackground != null)
        {
            geBackground.setOpacity(0);
            log.debug("Reset GE background opacity to default");
        }
    }

    private void applyBankTransparency()
    {
        Widget bankFrame = client.getWidget(BANK_INTERFACE_ID, BANK_BACKGROUND_CHILD_ID);

        if (bankFrame == null)
        {
            return;
        }

        Widget[] children = bankFrame.getDynamicChildren();
        if (children == null || children.length == 0)
        {
            return;
        }

        Widget bankBackground = children[0];
        if (bankBackground != null)
        {
            int opacity = config.hideBankBackground() ? 200 : 0;
            bankBackground.setOpacity(opacity);
            log.debug("Applied Bank background opacity: {}", opacity);
        }
    }

    private void resetBankTransparency()
    {
        Widget bankFrame = client.getWidget(BANK_INTERFACE_ID, BANK_BACKGROUND_CHILD_ID);

        if (bankFrame == null)
        {
            return;
        }

        Widget[] children = bankFrame.getDynamicChildren();
        if (children == null || children.length == 0)
        {
            return;
        }

        Widget bankBackground = children[0];
        if (bankBackground != null)
        {
            bankBackground.setOpacity(0);
            log.debug("Reset Bank background opacity to default");
        }
    }

    @Provides
    TransparentInterfacesConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TransparentInterfacesConfig.class);
    }
}
