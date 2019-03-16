/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.command;

import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.util.text.TextComponentTranslation;

public class Exceptions
{
    public static final DynamicCommandExceptionType COMPUTER_ARG_NONE = translated1( "argument.computercraft.computer.no_matching" );
    public static final Dynamic2CommandExceptionType COMPUTER_ARG_MANY = translated2( "argument.computercraft.computer.many_matching" );

    public static final DynamicCommandExceptionType TRACKING_FIELD_ARG_NONE = translated1( "argument.computercraft.tacking_field.no_field" );

    static final SimpleCommandExceptionType NOT_TRACKING_EXCEPTION = translated( "commands.computercraft.track.stop.not_enabled" );
    static final SimpleCommandExceptionType NO_TIMINGS_EXCEPTION = translated( "commands.computercraft.track.dump.no_timings" );

    static final SimpleCommandExceptionType TP_NOT_THERE = translated( "commands.computercraft.tp.not_there" );
    static final SimpleCommandExceptionType TP_NOT_PLAYER = translated( "commands.computercraft.tp.not_player" );

    public static final SimpleCommandExceptionType ARGUMENT_EXPECTED = translated( "argument.computercraft.argument_expected" );

    private static SimpleCommandExceptionType translated( String key )
    {
        return new SimpleCommandExceptionType( new TextComponentTranslation( key ) );
    }

    private static DynamicCommandExceptionType translated1( String key )
    {
        return new DynamicCommandExceptionType( x -> new TextComponentTranslation( key, x ) );
    }

    private static Dynamic2CommandExceptionType translated2( String key )
    {
        return new Dynamic2CommandExceptionType( ( x, y ) -> new TextComponentTranslation( key, x, y ) );
    }
}
