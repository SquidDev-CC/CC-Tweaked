/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.items;

import com.google.common.base.Objects;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.PocketUpgrades;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ClientComputer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.items.IComputerItem;
import dan200.computercraft.shared.pocket.apis.PocketAPI;
import dan200.computercraft.shared.pocket.core.PocketServerComputer;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemPocketComputer extends Item implements IComputerItem, IMedia, IColouredItem
{
    private static final String NBT_UPGRADE = "Upgrade";
    private static final String NBT_UPGRADE_INFO = "UpgradeInfo";
    public static final String NBT_LIGHT = "Light";

    private static final String NBT_INSTANCE = "Instanceid";
    private static final String NBT_SESSION = "SessionId";

    private final ComputerFamily family;

    public ItemPocketComputer( Properties settings, ComputerFamily family )
    {
        super( settings );
        this.family = family;
        addPropertyOverride( new ResourceLocation( ComputerCraft.MOD_ID, "state" ), COMPUTER_STATE );
        addPropertyOverride( new ResourceLocation( ComputerCraft.MOD_ID, "coloured" ), COMPUTER_COLOURED );
    }

    public ItemStack create( int id, String label, int colour, IPocketUpgrade upgrade )
    {
        ItemStack result = new ItemStack( this );
        if( id >= 0 ) result.getOrCreateTag().putInt( NBT_ID, id );
        if( label != null ) result.setDisplayName( new TextComponentString( label ) );
        if( upgrade != null ) result.getOrCreateTag().putString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        if( colour != -1 ) result.getOrCreateTag().putInt( NBT_COLOUR, colour );
        return result;
    }

    @Override
    public void fillItemGroup( @Nonnull ItemGroup group, @Nonnull NonNullList<ItemStack> stacks )
    {
        if( !isInGroup( group ) ) return;
        stacks.add( create( -1, null, -1, null ) );
        for( IPocketUpgrade upgrade : PocketUpgrades.getVanillaUpgrades() )
        {
            stacks.add( create( -1, null, -1, upgrade ) );
        }
    }

    @Override
    public void inventoryTick( ItemStack stack, World world, Entity entity, int slotNum, boolean selected )
    {
        if( !world.isRemote )
        {
            // Server side
            IInventory inventory = (entity instanceof EntityPlayer) ? ((EntityPlayer) entity).inventory : null;
            PocketServerComputer computer = createServerComputer( world, inventory, entity, stack );
            if( computer != null )
            {
                IPocketUpgrade upgrade = getUpgrade( stack );

                // Ping computer
                computer.keepAlive();
                computer.setWorld( world );
                computer.updateValues( entity, stack, upgrade );

                // Sync ID
                int id = computer.getID();
                if( id != getComputerID( stack ) )
                {
                    setComputerID( stack, id );
                    if( inventory != null )
                    {
                        inventory.markDirty();
                    }
                }

                // Sync label
                String label = computer.getLabel();
                if( !Objects.equal( label, getLabel( stack ) ) )
                {
                    setLabel( stack, label );
                    if( inventory != null )
                    {
                        inventory.markDirty();
                    }
                }

                // Update pocket upgrade
                if( upgrade != null )
                {
                    upgrade.update( computer, computer.getPeripheral( 2 ) );
                }
            }
        }
    }

    @Nonnull
    @Override
    public ActionResult<ItemStack> onItemRightClick( World world, EntityPlayer player, @Nonnull EnumHand hand )
    {
        ItemStack stack = player.getHeldItem( hand );
        if( !world.isRemote )
        {
            PocketServerComputer computer = createServerComputer( world, player.inventory, player, stack );

            boolean stop = false;
            if( computer != null )
            {
                computer.turnOn();

                IPocketUpgrade upgrade = getUpgrade( stack );
                if( upgrade != null )
                {
                    computer.updateValues( player, stack, upgrade );
                    stop = upgrade.onRightClick( world, computer, computer.getPeripheral( 2 ) );
                }
            }

            if( !stop ) ComputerCraft.openPocketComputerGUI( player, hand );
        }
        return new ActionResult<>( EnumActionResult.SUCCESS, stack );
    }

    @Nonnull
    @Override
    public ITextComponent getDisplayName( @Nonnull ItemStack stack )
    {
        String baseString = getTranslationKey( stack );
        IPocketUpgrade upgrade = getUpgrade( stack );
        if( upgrade != null )
        {
            return new TextComponentTranslation( baseString + ".upgraded",
                new TextComponentTranslation( upgrade.getUnlocalisedAdjective() )
            );
        }
        else
        {
            return super.getDisplayName( stack );
        }
    }

    @Override
    public void addInformation( @Nonnull ItemStack stack, @Nullable World world, List<ITextComponent> list, ITooltipFlag flag )
    {
        if( flag.isAdvanced() )
        {
            int id = getComputerID( stack );
            if( id >= 0 )
            {
                list.add( new TextComponentTranslation( "gui.computercraft.tooltip.computer_id", id )
                    .applyTextStyle( TextFormatting.GRAY ) );
            }
        }
    }

    @Nullable
    @Override
    public String getCreatorModId( ItemStack stack )
    {
        IPocketUpgrade upgrade = getUpgrade( stack );
        if( upgrade != null )
        {
            // If we're a non-vanilla, non-CC upgrade then return whichever mod this upgrade
            // belongs to.
            String mod = PocketUpgrades.getOwner( upgrade );
            if( mod != null && !mod.equals( ComputerCraft.MOD_ID ) ) return mod;
        }

        return super.getCreatorModId( stack );
    }

    public PocketServerComputer createServerComputer( final World world, IInventory inventory, Entity entity, @Nonnull ItemStack stack )
    {
        if( world.isRemote )
        {
            return null;
        }

        PocketServerComputer computer;
        int instanceID = getInstanceID( stack );
        int sessionID = getSessionID( stack );
        int correctSessionID = ComputerCraft.serverComputerRegistry.getSessionID();

        if( instanceID >= 0 && sessionID == correctSessionID &&
            ComputerCraft.serverComputerRegistry.contains( instanceID ) )
        {
            computer = (PocketServerComputer) ComputerCraft.serverComputerRegistry.get( instanceID );
        }
        else
        {
            if( instanceID < 0 || sessionID != correctSessionID )
            {
                instanceID = ComputerCraft.serverComputerRegistry.getUnusedInstanceID();
                setInstanceID( stack, instanceID );
                setSessionID( stack, correctSessionID );
            }
            int computerID = getComputerID( stack );
            if( computerID < 0 )
            {
                computerID = ComputerCraftAPI.createUniqueNumberedSaveDir( world, "computer" );
                setComputerID( stack, computerID );
            }
            computer = new PocketServerComputer(
                world,
                computerID,
                getLabel( stack ),
                instanceID,
                getFamily()
            );
            computer.updateValues( entity, stack, getUpgrade( stack ) );
            computer.addAPI( new PocketAPI( computer ) );
            ComputerCraft.serverComputerRegistry.add( instanceID, computer );
            if( inventory != null )
            {
                inventory.markDirty();
            }
        }
        computer.setWorld( world );
        return computer;
    }

    public static ServerComputer getServerComputer( @Nonnull ItemStack stack )
    {
        int instanceID = getInstanceID( stack );
        if( instanceID >= 0 )
        {
            return ComputerCraft.serverComputerRegistry.get( instanceID );
        }
        return null;
    }

    public static ClientComputer createClientComputer( @Nonnull ItemStack stack )
    {
        int instanceID = getInstanceID( stack );
        if( instanceID >= 0 )
        {
            if( !ComputerCraft.clientComputerRegistry.contains( instanceID ) )
            {
                ComputerCraft.clientComputerRegistry.add( instanceID, new ClientComputer( instanceID ) );
            }
            return ComputerCraft.clientComputerRegistry.get( instanceID );
        }
        return null;
    }

    private static ClientComputer getClientComputer( @Nonnull ItemStack stack )
    {
        int instanceID = getInstanceID( stack );
        if( instanceID >= 0 )
        {
            return ComputerCraft.clientComputerRegistry.get( instanceID );
        }
        return null;
    }

    // IComputerItem implementation

    private void setComputerID( @Nonnull ItemStack stack, int computerID )
    {
        stack.getOrCreateTag().putInt( NBT_ID, computerID );
    }

    @Override
    public String getLabel( @Nonnull ItemStack stack )
    {
        return IComputerItem.super.getLabel( stack );
    }

    @Override
    public ComputerFamily getFamily()
    {
        return family;
    }

    @Override
    public ItemStack withFamily( @Nonnull ItemStack stack, @Nonnull ComputerFamily family )
    {
        return PocketComputerItemFactory.create(
            getComputerID( stack ), getLabel( stack ), getColour( stack ),
            family, getUpgrade( stack )
        );
    }

    // IMedia

    @Override
    public boolean setLabel( @Nonnull ItemStack stack, String label )
    {
        if( label != null )
        {
            stack.setDisplayName( new TextComponentString( label ) );
        }
        else
        {
            stack.clearCustomName();
        }
        return true;
    }

    @Override
    public IMount createDataMount( @Nonnull ItemStack stack, @Nonnull World world )
    {
        int id = getComputerID( stack );
        if( id >= 0 )
        {
            return ComputerCraftAPI.createSaveDirMount( world, "computer/" + id, ComputerCraft.computerSpaceLimit );
        }
        return null;
    }

    private static int getInstanceID( @Nonnull ItemStack stack )
    {
        NBTTagCompound compound = stack.getTag();
        return compound != null && compound.contains( NBT_INSTANCE ) ? compound.getInt( NBT_INSTANCE ) : -1;
    }

    private static void setInstanceID( @Nonnull ItemStack stack, int instanceID )
    {
        stack.getOrCreateTag().putInt( NBT_INSTANCE, instanceID );
    }

    private static int getSessionID( @Nonnull ItemStack stack )
    {
        NBTTagCompound compound = stack.getTag();
        return compound != null && compound.contains( NBT_SESSION ) ? compound.getInt( NBT_SESSION ) : -1;
    }

    private static void setSessionID( @Nonnull ItemStack stack, int sessionID )
    {
        stack.getOrCreateTag().putInt( NBT_SESSION, sessionID );
    }

    @OnlyIn( Dist.CLIENT )
    public static ComputerState getState( @Nonnull ItemStack stack )
    {
        ClientComputer computer = getClientComputer( stack );
        if( computer != null && computer.isOn() )
        {
            return computer.isCursorDisplayed() ? ComputerState.BLINKING : ComputerState.ON;
        }
        return ComputerState.OFF;
    }

    @OnlyIn( Dist.CLIENT )
    public static int getLightState( @Nonnull ItemStack stack )
    {
        ClientComputer computer = getClientComputer( stack );
        if( computer != null && computer.isOn() )
        {
            NBTTagCompound computerNBT = computer.getUserData();
            if( computerNBT != null && computerNBT.contains( NBT_LIGHT ) )
            {
                return computerNBT.getInt( NBT_LIGHT );
            }
        }
        return -1;
    }

    public static IPocketUpgrade getUpgrade( @Nonnull ItemStack stack )
    {
        NBTTagCompound compound = stack.getTag();
        return compound != null && compound.contains( NBT_UPGRADE )
            ? PocketUpgrades.get( compound.getString( NBT_UPGRADE ) ) : null;

    }

    public static void setUpgrade( @Nonnull ItemStack stack, IPocketUpgrade upgrade )
    {
        NBTTagCompound compound = stack.getOrCreateTag();

        if( upgrade == null )
        {
            compound.remove( NBT_UPGRADE );
        }
        else
        {
            compound.putString( NBT_UPGRADE, upgrade.getUpgradeID().toString() );
        }

        compound.remove( NBT_UPGRADE_INFO );
    }

    public static NBTTagCompound getUpgradeInfo( @Nonnull ItemStack stack )
    {
        return stack.getOrCreateChildTag( NBT_UPGRADE_INFO );
    }

    private static final IItemPropertyGetter COMPUTER_STATE = ( stack, world, player ) -> getState( stack ).ordinal();
    private static final IItemPropertyGetter COMPUTER_COLOURED = ( stack, world, player ) -> IColouredItem.getColourBasic( stack ) != -1 ? 1 : 0;
}
