/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.peripheral.monitor;

import dan200.computercraft.shared.common.ClientTerminal;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ClientMonitor extends ClientTerminal
{
    private static final Set<ClientMonitor> allMonitors = new HashSet<>();

    private final TileMonitor origin;

    public long lastRenderFrame = -1;
    public BlockPos lastRenderPos = null;
    public int[] renderDisplayLists = null;

    public ClientMonitor( boolean colour, TileMonitor origin )
    {
        super( colour );
        this.origin = origin;
    }

    public TileMonitor getOrigin()
    {
        return origin;
    }

    @SideOnly( Side.CLIENT )
    public void createLists()
    {
        if( renderDisplayLists == null )
        {
            renderDisplayLists = new int[3];

            for( int i = 0; i < renderDisplayLists.length; i++ )
            {
                renderDisplayLists[i] = GlStateManager.glGenLists( 1 );
            }

            synchronized( allMonitors )
            {
                allMonitors.add( this );
            }
        }
    }

    @SideOnly( Side.CLIENT )
    public void destroy()
    {
        if( renderDisplayLists != null )
        {
            synchronized( allMonitors )
            {
                allMonitors.remove( this );
            }

            for( int list : renderDisplayLists )
            {
                GlStateManager.glDeleteLists( list, 1 );
            }

            renderDisplayLists = null;
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
                if( monitor.renderDisplayLists != null )
                {
                    for( int list : monitor.renderDisplayLists )
                    {
                        GlStateManager.glDeleteLists( list, 1 );
                    }
                    monitor.renderDisplayLists = null;
                }

                iterator.remove();
            }
        }
    }
}
