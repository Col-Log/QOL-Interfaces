package com.transparentui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Friend;
import net.runelite.api.NameableContainer;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.Text;

public class TradeOverlay extends Overlay
{
    // Trade main screen (first screen)
    private static final int TRADE_MAIN_GROUP = 335;
    private static final int TRADE_MAIN_TITLE_CHILD = 31; // "Trading with: PlayerName"
    private static final int TRADE_MAIN_WINDOW_CHILD = 0; // Main trade window container

    private final Client client;
    private final TransparentInterfacesConfig config;

    // Track what we've already modified to avoid re-processing
    private String lastModifiedPlayer = null;

    @Inject
    public TradeOverlay(Client client, TransparentInterfacesConfig config)
    {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.tradeFriendWarning())
        {
            lastModifiedPlayer = null;
            return null;
        }

        // Only show on first trade screen (not confirmation screen)
        Widget tradeMainTitle = client.getWidget(TRADE_MAIN_GROUP, TRADE_MAIN_TITLE_CHILD);
        if (tradeMainTitle == null || tradeMainTitle.isHidden())
        {
            lastModifiedPlayer = null;
            return null;
        }

        String titleText = tradeMainTitle.getText();
        if (titleText == null)
        {
            lastModifiedPlayer = null;
            return null;
        }

        // Extract just the player name (remove any tags and existing suffixes we may have added)
        String cleanTitle = Text.removeTags(titleText);

        // Check for "Trading with:" or "Trading With:" (case insensitive)
        String lowerTitle = cleanTitle.toLowerCase();
        if (!lowerTitle.startsWith("trading with:"))
        {
            lastModifiedPlayer = null;
            return null;
        }

        // Extract player name after "Trading with:" (13 characters)
        String playerName = cleanTitle.substring(13).trim();

        // Remove any existing suffix we added (in case of re-render)
        if (playerName.contains(" - ("))
        {
            playerName = playerName.substring(0, playerName.indexOf(" - (")).trim();
        }

        if (playerName.isEmpty())
        {
            return null;
        }

        // Check friend and clan status
        boolean isFriend = isOnFriendsList(playerName);
        boolean isClanMember = isInClan(playerName);

        // Build the new title with clan status suffix (colored)
        String clanSuffix = isClanMember
            ? " - <col=00ff00>(Same Clan)</col>"
            : " - <col=ff0000>(Not in Clan)</col>";
        String newTitleStr = "Trading with: " + playerName + clanSuffix;

        // Update the widget text if needed
        if (!newTitleStr.equals(lastModifiedPlayer))
        {
            tradeMainTitle.setText(newTitleStr);
            lastModifiedPlayer = newTitleStr;
        }

        // Draw the friend status message at bottom of trade window (independent of clan status)
        Widget tradeWindow = client.getWidget(TRADE_MAIN_GROUP, TRADE_MAIN_WINDOW_CHILD);
        if (tradeWindow != null)
        {
            renderFriendStatus(graphics, tradeWindow, playerName, isFriend);
        }

        return null;
    }

    private void renderFriendStatus(Graphics2D graphics, Widget anchorWidget, String playerName, boolean isFriend)
    {
        Rectangle widgetBounds = anchorWidget.getBounds();
        if (widgetBounds == null)
        {
            return;
        }

        String message;
        Color textColor;

        if (isFriend)
        {
            message = playerName + " is on your friends list";
            textColor = new Color(0, 255, 0); // Green
        }
        else
        {
            message = playerName + " is NOT on your friends list";
            textColor = new Color(255, 0, 0); // Red
        }

        // Draw the message inside the trade window near the bottom
        FontMetrics fm = graphics.getFontMetrics();
        int textWidth = fm.stringWidth(message);
        int textX = widgetBounds.x + (widgetBounds.width - textWidth) / 2;
        int textY = widgetBounds.y + widgetBounds.height - 35;

        // Draw shadow for better visibility
        graphics.setColor(Color.BLACK);
        graphics.drawString(message, textX + 1, textY + 1);

        // Draw main text
        graphics.setColor(textColor);
        graphics.drawString(message, textX, textY);
    }

    private boolean isOnFriendsList(String playerName)
    {
        NameableContainer<Friend> friendContainer = client.getFriendContainer();
        if (friendContainer == null)
        {
            return false;
        }

        String normalizedName = Text.standardize(playerName);

        for (Friend friend : friendContainer.getMembers())
        {
            if (friend != null)
            {
                String friendName = Text.standardize(friend.getName());
                if (friendName.equals(normalizedName))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isInClan(String playerName)
    {
        ClanChannel clanChannel = client.getClanChannel();
        if (clanChannel == null)
        {
            return false;
        }

        String normalizedName = Text.standardize(playerName);

        for (ClanChannelMember member : clanChannel.getMembers())
        {
            if (member != null)
            {
                String memberName = Text.standardize(member.getName());
                if (memberName.equals(normalizedName))
                {
                    return true;
                }
            }
        }

        return false;
    }
}
