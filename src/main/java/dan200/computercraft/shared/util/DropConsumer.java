/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.util;

import dan200.computercraft.ComputerCraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID )
public final class DropConsumer
{
    private DropConsumer()
    {
    }

    private static Function<ItemStack, ItemStack> dropConsumer;
    private static List<ItemStack> remainingDrops;
    private static WeakReference<World> dropWorld;
    private static AxisAlignedBB dropBounds;
    private static WeakReference<Entity> dropEntity;

    public static void set( Entity entity, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>();
        dropEntity = new WeakReference<>( entity );
        dropWorld = new WeakReference<>( entity.world );
        dropBounds = new AxisAlignedBB( entity.getPosition() ).grow( 2, 2, 2 );

        entity.captureDrops = true;
    }

    public static void set( World world, BlockPos pos, Function<ItemStack, ItemStack> consumer )
    {
        dropConsumer = consumer;
        remainingDrops = new ArrayList<>( 2 );
        dropEntity = null;
        dropWorld = new WeakReference<>( world );
        dropBounds = new AxisAlignedBB( pos ).grow( 2, 2, 2 );
    }

    public static List<ItemStack> clear()
    {
        if( dropEntity != null )
        {
            Entity entity = dropEntity.get();
            if( entity != null )
            {
                entity.captureDrops = false;
                if( entity.capturedDrops != null )
                {
                    for( EntityItem entityItem : entity.capturedDrops ) handleDrops( entityItem.getItem() );
                    entity.capturedDrops.clear();
                }
            }
        }

        List<ItemStack> remainingStacks = remainingDrops;

        dropConsumer = null;
        remainingDrops = null;
        dropEntity = null;
        dropWorld = null;
        dropBounds = null;

        return remainingStacks;
    }

    private static void handleDrops( ItemStack stack )
    {
        ItemStack remaining = dropConsumer.apply( stack );
        if( !remaining.isEmpty() ) remainingDrops.add( remaining );
    }

    @SubscribeEvent( priority = EventPriority.HIGHEST )
    public static void onEntitySpawn( EntityJoinWorldEvent event )
    {
        // Capture any nearby item spawns
        if( dropWorld != null && dropWorld.get() == event.getWorld() && event.getEntity() instanceof EntityItem
            && dropBounds.contains( event.getEntity().getPositionVector() ) )
        {
            handleDrops( ((EntityItem) event.getEntity()).getItem() );
            event.setCanceled( true );
        }
    }
}
