/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.client.gui.widgets.ComputerSidebar;
import dan200.computercraft.client.gui.widgets.WidgetTerminal;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.inventory.ContainerComputerBase;
import dan200.computercraft.shared.computer.upload.FileUpload;
import dan200.computercraft.shared.computer.upload.UploadResult;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.network.server.UploadFileMessage;
import net.minecraft.client.gui.screen.AlertScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class ComputerScreenBase<T extends ContainerComputerBase> extends ContainerScreen<T>
{
    private static final ITextComponent OK = new TranslationTextComponent( "gui.computercraft.button.ok" );

    protected WidgetTerminal terminal;
    protected final ClientComputer computer;
    protected final ComputerFamily family;

    protected final int sidebarYOffset;

    public ComputerScreenBase( T container, PlayerInventory player, ITextComponent title, int sidebarYOffset )
    {
        super( container, player, title );
        computer = (ClientComputer) container.getComputer();
        family = container.getFamily();
        this.sidebarYOffset = sidebarYOffset;
    }

    protected abstract WidgetTerminal createTerminal();

    @Override
    protected final void init()
    {
        super.init();
        minecraft.keyboardHandler.setSendRepeatsToGui( true );

        terminal = addButton( createTerminal() );
        ComputerSidebar.addButtons( this, computer, this::addButton, leftPos, topPos + sidebarYOffset );
        setFocused( terminal );
    }

    @Override
    public final void removed()
    {
        super.removed();
        minecraft.keyboardHandler.setSendRepeatsToGui( false );
    }

    @Override
    public final void tick()
    {
        super.tick();
        terminal.update();
    }

    @Override
    public final boolean keyPressed( int key, int scancode, int modifiers )
    {
        // Forward the tab key to the terminal, rather than moving between controls.
        if( key == GLFW.GLFW_KEY_TAB && getFocused() != null && getFocused() == terminal )
        {
            return getFocused().keyPressed( key, scancode, modifiers );
        }

        return super.keyPressed( key, scancode, modifiers );
    }


    @Override
    public final void render( @Nonnull MatrixStack stack, int mouseX, int mouseY, float partialTicks )
    {
        renderBackground( stack );
        super.render( stack, mouseX, mouseY, partialTicks );
        renderTooltip( stack, mouseX, mouseY );
    }

    @Override
    public final boolean mouseDragged( double x, double y, int button, double deltaX, double deltaY )
    {
        return (getFocused() != null && getFocused().mouseDragged( x, y, button, deltaX, deltaY ))
            || super.mouseDragged( x, y, button, deltaX, deltaY );
    }


    @Override
    protected void renderLabels( @Nonnull MatrixStack transform, int mouseX, int mouseY )
    {
        // Skip rendering labels.
    }

    @Override
    public void onFilesDrop( @Nonnull List<Path> files )
    {
        if( files.isEmpty() ) return;

        if( computer == null || !computer.isOn() )
        {
            alert( UploadResult.FAILED_TITLE, UploadResult.COMPUTER_OFF_MSG );
            return;
        }

        long size = 0;

        List<FileUpload> toUpload = new ArrayList<>();
        for( Path file : files )
        {
            // TODO: Recurse directories? If so, we probably want to shunt this off-thread.
            if( !Files.isRegularFile( file ) ) continue;

            try( SeekableByteChannel sbc = Files.newByteChannel( file ) )
            {
                long fileSize = sbc.size();
                if( fileSize > UploadFileMessage.MAX_SIZE || (size += fileSize) >= UploadFileMessage.MAX_SIZE )
                {
                    alert( UploadResult.FAILED_TITLE, UploadResult.TOO_MUCH_MSG );
                    return;
                }

                ByteBuffer buffer = ByteBuffer.allocateDirect( (int) fileSize );
                int read = sbc.read( buffer );
                buffer.limit( read );
                buffer.position( 0 );

                toUpload.add( new FileUpload( file.getFileName().toString(), buffer ) );
            }
            catch( IOException e )
            {
                ComputerCraft.log.error( "Failed uploading files", e );
                alert( UploadResult.FAILED_TITLE, new TranslationTextComponent( "computercraft.gui.upload.failed.generic", e.getMessage() ) );
            }
        }

        if( toUpload.size() > 0 )
        {
            NetworkHandler.sendToServer( new UploadFileMessage( computer.getInstanceID(), toUpload ) );
        }
    }

    public void uploadResult( UploadResult result, ITextComponent message )
    {
        switch( result )
        {
            case SUCCESS:
                alert( UploadResult.SUCCESS_TITLE, message );
                break;
            case ERROR:
                alert( UploadResult.FAILED_TITLE, message );
                break;
            case CONFIRM_OVERWRITE:
                minecraft.setScreen( new ConfirmScreen(
                    confirm -> minecraft.setScreen( this ),
                    UploadResult.UPLOAD_OVERWRITE, message
                ) );
                break;
        }
    }

    private void alert( ITextComponent title, ITextComponent message )
    {
        minecraft.setScreen( new AlertScreen( () -> minecraft.setScreen( this ), title, message, OK ) );
    }
}
