/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.common.IDirectionalTile;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.PeripheralType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

import javax.annotation.Nonnull;

public abstract class TilePeripheralBase extends TileGeneric implements IPeripheralTile, ITickable, IDirectionalTile
{
    // Statics

    protected EnumFacing m_dir;
    private int m_anim;
    private boolean m_changed;

    public TilePeripheralBase()
    {
        m_dir = EnumFacing.NORTH;
        m_anim = 0;
        m_changed = false;
    }


    @Override
    public BlockPeripheralBase getBlock()
    {
        return (BlockPeripheralBase) super.getBlock();
    }

    // IPeripheralTile implementation

    public final PeripheralType getPeripheralType()
    {
        return getBlock().getPeripheralType( getBlockState() );
    }

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return null;
    }

    // IDirectionalTile implementation

    @Override
    public EnumFacing getDirection()
    {
        return m_dir;
    }

    public EnumFacing getCachedDirection()
    {
        return m_dir;
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        if( dir != m_dir )
        {
            m_dir = dir;
            m_changed = true;
        }
    }

    public synchronized int getAnim()
    {
        return m_anim;
    }

    public synchronized void setAnim( int anim )
    {
        if( anim != m_anim )
        {
            m_anim = anim;
            m_changed = true;
        }
    }

    @Override
    public synchronized void update()
    {
        if( m_changed )
        {
            updateBlock();
            m_changed = false;
        }
    }

    @Override
    public void readFromNBT( NBTTagCompound nbttagcompound )
    {
        // Read properties
        super.readFromNBT( nbttagcompound );
        if( nbttagcompound.hasKey( "dir" ) )
        {
            m_dir = EnumFacing.byIndex( nbttagcompound.getInteger( "dir" ) );
        }
        if( nbttagcompound.hasKey( "anim" ) )
        {
            m_anim = nbttagcompound.getInteger( "anim" );
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound writeToNBT( NBTTagCompound nbttagcompound )
    {
        // Write properties
        nbttagcompound = super.writeToNBT( nbttagcompound );
        nbttagcompound.setInteger( "dir", m_dir.getIndex() );
        nbttagcompound.setInteger( "anim", m_anim );
        return nbttagcompound;
    }

    @Override
    public void readDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.readDescription( nbttagcompound );
        m_dir = EnumFacing.byIndex( nbttagcompound.getInteger( "dir" ) );
        m_anim = nbttagcompound.getInteger( "anim" );
    }

    @Override
    public void writeDescription( @Nonnull NBTTagCompound nbttagcompound )
    {
        super.writeDescription( nbttagcompound );
        nbttagcompound.setInteger( "dir", m_dir.getIndex() );
        nbttagcompound.setInteger( "anim", m_anim );
    }
}
