/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.client.gui.FixedWidthFontRenderer;
import dan200.computercraft.shared.common.ClientTerminal;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class ClientMonitor extends ClientTerminal
{
    private static final Set<ClientMonitor> allMonitors = new HashSet<>();

    private final TileMonitor origin;

    public long lastRenderFrame = -1;
    public BlockPos lastRenderPos = null;

    public VertexBuffer buffer;
    public int displayList = 0;

    public ClientMonitor( boolean colour, TileMonitor origin )
    {
        super( colour );
        this.origin = origin;
    }

    public TileMonitor getOrigin()
    {
        return origin;
    }

    /**
     * Create the appropriate buffer if needed.
     *
     * @param renderer The renderer to use. This can be fetched from {@link #renderer()}.
     * @return If a buffer was created. This will return {@code false} if we already have an appropriate buffer,
     * or this mode does not require one.
     */
    @SideOnly( Side.CLIENT )
    public boolean createBuffer( MonitorRenderer renderer )
    {
        switch( renderer )
        {
            case VBO:
                if( buffer != null ) return false;

                deleteBuffers();
                buffer = new VertexBuffer( FixedWidthFontRenderer.POSITION_COLOR_TEX );
                addMonitor();
                return true;
            case DISPLAY_LIST:
                if( displayList != 0 ) return false;

                deleteBuffers();
                displayList = GLAllocation.generateDisplayLists( 1 );
                addMonitor();
                return true;

            default:
                return false;
        }
    }

    private void addMonitor()
    {
        synchronized( allMonitors )
        {
            allMonitors.add( this );
        }
    }

    private void deleteBuffers()
    {
        if( buffer != null )
        {
            buffer.deleteGlBuffers();
            buffer = null;
        }

        if( displayList != 0 )
        {
            GLAllocation.deleteDisplayLists( displayList );
            displayList = 0;
        }
    }

    @SideOnly( Side.CLIENT )
    public void destroy()
    {
        if( buffer != null || displayList != 0 )
        {
            synchronized( allMonitors )
            {
                allMonitors.remove( this );
            }

            deleteBuffers();
        }
    }

    @SideOnly( Side.CLIENT )
    public static void destroyAll()
    {
        synchronized( allMonitors )
        {
            for( Iterator<ClientMonitor> iterator = allMonitors.iterator(); iterator.hasNext(); )
            {
                ClientMonitor monitor = iterator.next();
                monitor.deleteBuffers();

                iterator.remove();
            }
        }
    }
}
