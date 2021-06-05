/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.client.render.ComputerBorderRenderer;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.inventory.ContainerViewComputer;
import dan200.computercraft.shared.pocket.inventory.ContainerPocketComputer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;

import static dan200.computercraft.client.render.ComputerBorderRenderer.BORDER;

public final class GuiComputer<T extends ContainerComputerBase> extends ContainerScreen<T>
{
    private final ComputerFamily family;
    private final ClientComputer computer;
    private final int termWidth;
    private final int termHeight;

    private WidgetTerminal terminal = null;

    private GuiComputer(
        T container, PlayerInventory player, ITextComponent title, int termWidth, int termHeight
    )
    {
        super( container, player, title );
        family = container.getFamily();
        computer = (ClientComputer) container.getComputer();
        this.termWidth = termWidth;
        this.termHeight = termHeight;

        imageWidth = WidgetTerminal.getWidth( termWidth ) + BORDER * 2 + ComputerSidebar.WIDTH;
        imageHeight = WidgetTerminal.getHeight( termHeight ) + BORDER * 2;
    }

    public static GuiComputer<ContainerComputer> create( ContainerComputer container, PlayerInventory inventory, ITextComponent component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            ComputerCraft.computerTermWidth, ComputerCraft.computerTermHeight
        );
    }

    public static GuiComputer<ContainerPocketComputer> createPocket( ContainerPocketComputer container, PlayerInventory inventory, ITextComponent component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight
        );
    }

    public static GuiComputer<ContainerViewComputer> createView( ContainerViewComputer container, PlayerInventory inventory, ITextComponent component )
    {
        return new GuiComputer<>(
            container, inventory, component,
            container.getWidth(), container.getHeight()
        );
    }


    @Override
    protected void init()
    {
        super.init();

        minecraft.keyboardHandler.setSendRepeatsToGui( true );

        terminal = addButton( new WidgetTerminal( computer,
            leftPos + ComputerSidebar.WIDTH + BORDER, topPos + BORDER, termWidth, termHeight
        ) );
        ComputerSidebar.addButtons( this, computer, this::addButton, leftPos, topPos + BORDER );
        setFocused( terminal );
    }

    @Override
    public void removed()
    {
        super.removed();
        minecraft.keyboardHandler.setSendRepeatsToGui( false );
    }

    @Override
    public void tick()
    {
        super.tick();
        terminal.update();
    }

    @Override
    public boolean keyPressed( int key, int scancode, int modifiers )
    {
        // Forward the tab key to the terminal, rather than moving between controls.
        if( key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal )
        {
            return getFocused().keyPressed( key, scancode, modifiers );
        }

        return super.keyPressed( key, scancode, modifiers );
    }

    @Override
    public void renderBg( float partialTicks, int mouseX, int mouseY )
    {
        // Draw a border around the terminal
        RenderSystem.color4f( 1, 1, 1, 1 );
        minecraft.getTextureManager().bind( ComputerBorderRenderer.getTexture( family ) );

        ComputerBorderRenderer.render( terminal.x, terminal.y, getBlitOffset(), terminal.getWidth(), terminal.getHeight() );
        ComputerSidebar.renderBackground( leftPos, topPos + BORDER );
    }

    @Override
    public void render( int mouseX, int mouseY, float partialTicks )
    {
        renderBackground();
        super.render( mouseX, mouseY, partialTicks );
        renderTooltip( mouseX, mouseY );

        for( Widget widget : buttons )
        {
            if( widget.isHovered() ) widget.renderToolTip( mouseX, mouseY );
        }
    }

    @Override
    public boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }
}
