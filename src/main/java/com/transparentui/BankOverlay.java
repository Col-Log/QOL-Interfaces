package com.transparentui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class BankOverlay extends Overlay implements MouseListener
{
    private static final int BANK_INTERFACE_ID = InterfaceID.BANK;

    private static final int MAINMODAL_GROUP = 161;
    private static final int MAINMODAL_CHILD = 16;

    private static final int DRAG_BUTTON_WIDTH = 40;
    private static final int DRAG_BUTTON_HEIGHT = 20;

    private final Client client;
    private final TransparentInterfacesConfig config;

    // Store the cumulative offset from default
    private int offsetX = 0;
    private int offsetY = 0;

    // Store the base position (captured once when Bank opens)
    private int baseRelX = -1;
    private int baseRelY = -1;

    // Drag state
    private boolean isDragging = false;
    private Point dragStartMouse = null;
    private int dragStartOffsetX = 0;
    private int dragStartOffsetY = 0;

    // Cached drag button bounds
    private Rectangle dragButtonBounds = null;

    @Inject
    public BankOverlay(Client client, TransparentInterfacesConfig config)
    {
        this.client = client;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    /**
     * Called from plugin on every game tick to maintain position
     */
    public void onGameTick()
    {
        if (!config.moveableBank())
        {
            return;
        }

        applyOffset();
    }

    /**
     * Called right before each frame renders - this is the key to preventing flicker
     */
    public void onBeforeRender()
    {
        if (!config.moveableBank())
        {
            return;
        }

        applyOffset();
    }

    private void applyOffset()
    {
        if (offsetX == 0 && offsetY == 0)
        {
            return;
        }

        Widget bankWindow = client.getWidget(BANK_INTERFACE_ID, 0);
        if (bankWindow == null || bankWindow.isHidden())
        {
            return;
        }

        Widget mainModal = client.getWidget(MAINMODAL_GROUP, MAINMODAL_CHILD);
        if (mainModal == null)
        {
            return;
        }

        // Capture base position once
        if (baseRelX == -1)
        {
            baseRelX = mainModal.getRelativeX();
            baseRelY = mainModal.getRelativeY();
        }

        // Apply offset directly using setRelativeX/Y
        mainModal.setRelativeX(baseRelX + offsetX);
        mainModal.setRelativeY(baseRelY + offsetY);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.moveableBank())
        {
            if (offsetX != 0 || offsetY != 0)
            {
                resetPosition();
            }
            dragButtonBounds = null;
            return null;
        }

        Widget bankWindow = client.getWidget(BANK_INTERFACE_ID, 0);
        if (bankWindow == null || bankWindow.isHidden())
        {
            dragButtonBounds = null;
            // Reset base position when Bank closes
            baseRelX = -1;
            baseRelY = -1;
            return null;
        }

        Widget mainModal = client.getWidget(MAINMODAL_GROUP, MAINMODAL_CHILD);
        if (mainModal == null)
        {
            dragButtonBounds = null;
            return null;
        }

        // Capture base position once when Bank first opens
        if (baseRelX == -1)
        {
            baseRelX = mainModal.getRelativeX();
            baseRelY = mainModal.getRelativeY();
        }

        // Apply offset every frame
        if (offsetX != 0 || offsetY != 0)
        {
            mainModal.setRelativeX(baseRelX + offsetX);
            mainModal.setRelativeY(baseRelY + offsetY);
        }

        // Get bank window bounds to position drag button
        Rectangle bankBounds = bankWindow.getBounds();
        if (bankBounds == null)
        {
            dragButtonBounds = null;
            return null;
        }

        // Position drag button between GP counter and title text (in the title bar area)
        int buttonX = bankBounds.x + 115;
        int buttonY = bankBounds.y + 8;

        dragButtonBounds = new Rectangle(buttonX, buttonY, DRAG_BUTTON_WIDTH, DRAG_BUTTON_HEIGHT);

        // Draw the drag button
        Color bgColor = isDragging ? new Color(80, 60, 40) : new Color(60, 50, 40);
        graphics.setColor(bgColor);
        graphics.fillRect(buttonX, buttonY, DRAG_BUTTON_WIDTH, DRAG_BUTTON_HEIGHT);

        // Border
        graphics.setColor(new Color(100, 80, 60));
        graphics.drawRect(buttonX, buttonY, DRAG_BUTTON_WIDTH - 1, DRAG_BUTTON_HEIGHT - 1);

        // Text
        graphics.setColor(new Color(255, 152, 31)); // OSRS orange
        FontMetrics fm = graphics.getFontMetrics();
        String text = "Drag";
        int textX = buttonX + (DRAG_BUTTON_WIDTH - fm.stringWidth(text)) / 2;
        int textY = buttonY + (DRAG_BUTTON_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
        graphics.drawString(text, textX, textY);

        return null;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent e)
    {
        if (!config.moveableBank() || dragButtonBounds == null)
        {
            return e;
        }

        if (dragButtonBounds.contains(e.getPoint()))
        {
            isDragging = true;
            dragStartMouse = e.getPoint();
            dragStartOffsetX = offsetX;
            dragStartOffsetY = offsetY;
            e.consume();
        }

        return e;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent e)
    {
        if (isDragging)
        {
            isDragging = false;
            dragStartMouse = null;
            e.consume();
        }
        return e;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent e)
    {
        if (!isDragging || dragStartMouse == null)
        {
            return e;
        }

        Point currentMouse = e.getPoint();
        int deltaX = currentMouse.x - dragStartMouse.x;
        int deltaY = currentMouse.y - dragStartMouse.y;

        offsetX = dragStartOffsetX + deltaX;
        offsetY = dragStartOffsetY + deltaY;

        // Apply immediately during drag
        applyOffset();

        e.consume();
        return e;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent e)
    {
        return e;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent e)
    {
        return e;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent e)
    {
        return e;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent e)
    {
        return e;
    }

    public void resetPosition()
    {
        offsetX = 0;
        offsetY = 0;
        baseRelX = -1;
        baseRelY = -1;
        isDragging = false;
        dragStartMouse = null;
        dragButtonBounds = null;

        Widget mainModal = client.getWidget(MAINMODAL_GROUP, MAINMODAL_CHILD);
        if (mainModal != null)
        {
            mainModal.revalidate();
        }

        Widget bankWindow = client.getWidget(BANK_INTERFACE_ID, 0);
        if (bankWindow != null)
        {
            bankWindow.revalidate();
        }
    }
}
