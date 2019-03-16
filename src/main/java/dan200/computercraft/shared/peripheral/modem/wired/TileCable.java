/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.network.wired.IWiredElement;
import dan200.computercraft.api.network.wired.IWiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.command.CommandCopy;
import dan200.computercraft.shared.common.TileGeneric;
import dan200.computercraft.shared.peripheral.IPeripheralTile;
import dan200.computercraft.shared.peripheral.modem.ModemState;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.NamedBlockEntityType;
import dan200.computercraft.shared.util.TickScheduler;
import dan200.computercraft.shared.wired.CapabilityWiredElement;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

public class TileCable extends TileGeneric implements IPeripheralTile
{
    public static final NamedBlockEntityType<TileCable> FACTORY = NamedBlockEntityType.create(
        new ResourceLocation( ComputerCraft.MOD_ID, "cable" ),
        TileCable::new
    );

    private static final String NBT_PERIPHERAL_ENABLED = "PeirpheralAccess";

    private class CableElement extends WiredModemElement
    {
        @Nonnull
        @Override
        public World getWorld()
        {
            return TileCable.this.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = TileCable.this.getPos();
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }

        @Override
        protected void attachPeripheral( String name, IPeripheral peripheral )
        {
            m_modem.attachPeripheral( name, peripheral );
        }

        @Override
        protected void detachPeripheral( String name )
        {
            m_modem.detachPeripheral( name );
        }
    }

    private boolean m_peripheralAccessAllowed;
    private WiredModemLocalPeripheral m_peripheral = new WiredModemLocalPeripheral();

    private boolean m_destroyed = false;

    private EnumFacing modemDirection;
    private boolean hasModemDirection = false;
    private EnumFacing m_direction = EnumFacing.NORTH;
    private boolean m_connectionsFormed = false;

    private final WiredModemElement m_cable = new CableElement();
    private LazyOptional<IWiredElement> m_cableCapability = LazyOptional.of( () -> m_cable );
    private final IWiredNode m_node = m_cable.getNode();
    private final WiredModemPeripheral m_modem = new WiredModemPeripheral(
        new ModemState( () -> TickScheduler.schedule( this ) ),
        m_cable
    )
    {
        @Nonnull
        @Override
        protected WiredModemLocalPeripheral getLocalPeripheral()
        {
            return m_peripheral;
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = getPos().offset( m_direction );
            return new Vec3d( pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5 );
        }
    };

    public TileCable()
    {
        super( FACTORY );
    }

    private void onRemove()
    {
        if( world == null || !world.isRemote )
        {
            m_node.remove();
            m_connectionsFormed = false;
        }
    }

    @Override
    public void destroy()
    {
        if( !m_destroyed )
        {
            m_destroyed = true;
            m_modem.destroy();
            onRemove();
        }
    }

    @Override
    public void onChunkUnloaded()
    {
        super.onChunkUnloaded();
        onRemove();
    }

    @Override
    public void remove()
    {
        super.remove();
        onRemove();
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        if( !world.isRemote )
        {
            updateDirection();
            world.getPendingBlockTicks().scheduleTick( pos, getBlockState().getBlock(), 0 );
        }
    }

    @Override
    public void updateContainingBlockInfo()
    {
        super.updateContainingBlockInfo();
        hasModemDirection = false;
        if( !world.isRemote ) world.getPendingBlockTicks().scheduleTick( pos, getBlockState().getBlock(), 0 );
    }

    private void updateDirection()
    {
        if( !hasModemDirection )
        {
            hasModemDirection = true;
            modemDirection = getDirection();
        }
    }

    private EnumFacing getDirection()
    {
        IBlockState state = getBlockState();
        EnumFacing facing = state.get( BlockCable.MODEM ).getFacing();
        return facing != null ? facing : EnumFacing.NORTH;
    }

    @Override
    public void onNeighbourChange( @Nonnull BlockPos neighbour )
    {
        EnumFacing dir = getDirection();
        if( neighbour.equals( getPos().offset( dir ) ) && hasModem()
            && getWorld().getBlockState( neighbour ).getBlockFaceShape( world, neighbour, dir.getOpposite() ) != BlockFaceShape.SOLID
        )
        {
            if( hasCable() )
            {
                // Drop the modem and convert to cable
                Block.spawnAsEntity( getWorld(), getPos(), new ItemStack( ComputerCraft.Items.wiredModem ) );
                getWorld().setBlockState( getPos(), getBlockState().with( BlockCable.MODEM, CableModemVariant.None ) );
                modemChanged();
                connectionsChanged();
            }
            else
            {
                // Drop everything and remove block
                Block.spawnAsEntity( getWorld(), getPos(), new ItemStack( ComputerCraft.Items.wiredModem ) );
                getWorld().removeBlock( getPos() );
                // This'll call #destroy(), so we don't need to reset the network here.
            }

            return;
        }

        onNeighbourTileEntityChange( neighbour );
    }

    @Override
    public void onNeighbourTileEntityChange( @Nonnull BlockPos neighbour )
    {
        super.onNeighbourTileEntityChange( neighbour );
        if( !world.isRemote && m_peripheralAccessAllowed )
        {
            EnumFacing facing = getDirection();
            if( getPos().offset( facing ).equals( neighbour ) )
            {
                if( m_peripheral.attach( world, getPos(), facing ) ) updateConnectedPeripherals();
            }
        }
    }

    @Override
    public boolean onActivate( EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ )
    {
        if( !canAttachPeripheral() || player.isSneaking() ) return false;

        if( getWorld().isRemote ) return true;

        String oldName = m_peripheral.getConnectedName();
        togglePeripheralAccess();
        String newName = m_peripheral.getConnectedName();
        if( !Objects.equal( newName, oldName ) )
        {
            if( oldName != null )
            {
                player.sendStatusMessage( new TextComponentTranslation( "chat.computercraft.wired_modem.peripheral_disconnected",
                    CommandCopy.createCopyText( oldName ) ), false );
            }
            if( newName != null )
            {
                player.sendStatusMessage( new TextComponentTranslation( "chat.computercraft.wired_modem.peripheral_connected",
                    CommandCopy.createCopyText( newName ) ), false );
            }
        }

        return true;
    }

    @Override
    public void read( NBTTagCompound nbt )
    {
        super.read( nbt );
        m_peripheralAccessAllowed = nbt.getBoolean( NBT_PERIPHERAL_ENABLED );
        m_peripheral.read( nbt, "" );
    }

    @Nonnull
    @Override
    public NBTTagCompound write( NBTTagCompound nbt )
    {
        nbt.putBoolean( NBT_PERIPHERAL_ENABLED, m_peripheralAccessAllowed );
        m_peripheral.write( nbt, "" );
        return super.write( nbt );
    }

    private void updateBlockState()
    {
        IBlockState state = getBlockState();
        CableModemVariant oldVariant = state.get( BlockCable.MODEM );
        CableModemVariant newVariant = CableModemVariant
            .from( oldVariant.getFacing(), m_modem.getModemState().isOpen(), m_peripheralAccessAllowed );

        if( oldVariant != newVariant )
        {
            world.setBlockState( getPos(), state.with( BlockCable.MODEM, newVariant ) );
        }
    }

    @Override
    public void blockTick()
    {
        super.blockTick();
        updateDirection();

        if( getWorld().isRemote ) return;

        updateDirection();

        if( m_modem.getModemState().pollChanged() ) updateBlockState();

        if( !m_connectionsFormed )
        {
            m_connectionsFormed = true;

            connectionsChanged();
            if( m_peripheralAccessAllowed )
            {
                m_peripheral.attach( world, pos, modemDirection );
                updateConnectedPeripherals();
            }
        }
    }

    public void connectionsChanged()
    {
        if( getWorld().isRemote ) return;

        IBlockState state = getBlockState();
        World world = getWorld();
        BlockPos current = getPos();
        for( EnumFacing facing : DirectionUtil.FACINGS )
        {
            BlockPos offset = current.offset( facing );
            if( !world.isBlockLoaded( offset ) ) continue;

            IWiredElement element = ComputerCraftAPI.getWiredElementAt( world, offset, facing.getOpposite() );
            if( element == null ) continue;

            if( BlockCable.canConnectIn( state, facing ) )
            {
                // If we can connect to it then do so
                m_node.connectTo( element.getNode() );
            }
            else if( m_node.getNetwork() == element.getNode().getNetwork() )
            {
                // Otherwise if we're on the same network then attempt to void it.
                m_node.disconnectFrom( element.getNode() );
            }
        }
    }

    public void modemChanged()
    {
        // Tell anyone who cares that the connection state has changed
        // TODO: Be more restrictive about this.
        m_cableCapability.invalidate();
        m_cableCapability = LazyOptional.of( () -> m_cable );

        if( getWorld().isRemote ) return;

        // If we can no longer attach peripherals, then detach any
        // which may have existed
        if( !canAttachPeripheral() && m_peripheralAccessAllowed )
        {
            m_peripheralAccessAllowed = false;
            m_peripheral.detach();
            m_node.updatePeripherals( Collections.emptyMap() );
            markDirty();
            updateBlockState();
        }
    }

    // private stuff
    private void togglePeripheralAccess()
    {
        if( !m_peripheralAccessAllowed )
        {
            m_peripheral.attach( world, getPos(), getDirection() );
            if( !m_peripheral.hasPeripheral() ) return;

            m_peripheralAccessAllowed = true;
            m_node.updatePeripherals( m_peripheral.toMap() );
        }
        else
        {
            m_peripheral.detach();

            m_peripheralAccessAllowed = false;
            m_node.updatePeripherals( Collections.emptyMap() );
        }

        updateBlockState();
    }

    private void updateConnectedPeripherals()
    {
        Map<String, IPeripheral> peripherals = m_peripheral.toMap();
        if( peripherals.isEmpty() )
        {
            // If there are no peripherals then disable access and update the display state.
            m_peripheralAccessAllowed = false;
            updateBlockState();
        }

        m_node.updatePeripherals( peripherals );
    }

    @Override
    public boolean canRenderBreaking()
    {
        return true;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> capability, @Nullable EnumFacing facing )
    {
        if( capability == CapabilityWiredElement.CAPABILITY )
        {
            return !m_destroyed && BlockCable.canConnectIn( getBlockState(), facing )
                ? m_cableCapability.cast() : LazyOptional.empty();
        }

        return super.getCapability( capability, facing );
    }

    @Override
    public IPeripheral getPeripheral( EnumFacing side )
    {
        return !m_destroyed && hasModem() && side == getDirection() ? m_modem : null;
    }

    public boolean hasCable()
    {
        return getBlockState().get( BlockCable.CABLE );
    }

    public boolean hasModem()
    {
        return getBlockState().get( BlockCable.MODEM ) != CableModemVariant.None;
    }

    boolean canAttachPeripheral()
    {
        return hasCable() && hasModem();
    }
}
