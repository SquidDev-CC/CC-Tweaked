/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.computer;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.core.filesystem.FileMount;
import dan200.computercraft.core.filesystem.JarMount;
import dan200.computercraft.core.filesystem.MemoryMount;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * A very basic environment
 */
public class BasicEnvironment implements IComputerEnvironment
{
    private final IWritableMount mount;

    public BasicEnvironment()
    {
        this( new MemoryMount() );
    }

    public BasicEnvironment( IWritableMount mount )
    {
        this.mount = mount;
    }

    @Override
    public int assignNewID()
    {
        return 0;
    }

    @Override
    public IWritableMount createSaveDirMount( String path, long space )
    {
        return mount;
    }

    @Override
    public int getDay()
    {
        return 0;
    }

    @Override
    public double getTimeOfDay()
    {
        return 0;
    }

    @Override
    public boolean isColour()
    {
        return true;
    }

    @Override
    public long getComputerSpaceLimit()
    {
        return ComputerCraft.computerSpaceLimit;
    }

    @Override
    public String getHostString()
    {
        return "ComputerCraft ${version} (Minecraft " + Loader.MC_VERSION + ")";
    }

    @Override
    @Deprecated
    public IMount createResourceMount( String domain, String subPath )
    {
        File file = getContainingFile();

        String path = "assets/" + domain + "/" + subPath;

        if( file.isFile() )
        {
            try
            {
                return new JarMount( file, path );
            }
            catch( IOException e )
            {
                throw new UncheckedIOException( e );
            }
        }
        else
        {
            File wholeFile = new File( file, path );

            // If we don't exist, walk up the tree looking for resource folders
            File baseFile = file;
            while( baseFile != null && !wholeFile.exists() )
            {
                baseFile = baseFile.getParentFile();
                wholeFile = new File( baseFile, "resources/main/" + path );
            }

            if( !wholeFile.exists() ) throw new IllegalStateException( "Cannot find ROM mount at " + file );

            return new FileMount( wholeFile, 0 );
        }
    }

    @Override
    public InputStream createResourceFile( String domain, String subPath )
    {
        return ComputerCraft.class.getClassLoader().getResourceAsStream( "assets/" + domain + "/" + subPath );
    }

    private static File getContainingFile()
    {
        String path = ComputerCraft.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        int bangIndex = path.indexOf( "!" );

        // Plain old file, so step up from dan200.computercraft.
        if( bangIndex < 0 ) return new File( path );

        path = path.substring( 0, bangIndex );
        URL url;
        try
        {
            url = new URL( path );
        }
        catch( MalformedURLException e )
        {
            throw new IllegalStateException( e );
        }

        try
        {
            return new File( url.toURI() );
        }
        catch( URISyntaxException e )
        {
            return new File( url.getPath() );
        }
    }
}
