/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.core.apis.http.AddressRule;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import dan200.computercraft.shared.peripheral.monitor.MonitorRenderer;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.minecraftforge.common.ForgeConfigSpec.Builder;
import static net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

@Mod.EventBusSubscriber( modid = ComputerCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public final class Config
{
    private static final int MODEM_MAX_RANGE = 100000;

    private static final String TRANSLATION_PREFIX = "gui.computercraft.config.";

    private static final ConfigValue<Integer> computerSpaceLimit;
    private static final ConfigValue<Integer> floppySpaceLimit;
    private static final ConfigValue<Integer> maximumFilesOpen;
    private static final ConfigValue<Boolean> disableLua51Features;
    private static final ConfigValue<String> defaultComputerSettings;
    private static final ConfigValue<Boolean> debugEnabled;
    private static final ConfigValue<Boolean> logComputerErrors;

    private static final ConfigValue<Integer> computerThreads;
    private static final ConfigValue<Integer> maxMainGlobalTime;
    private static final ConfigValue<Integer> maxMainComputerTime;

    private static final ConfigValue<Boolean> httpEnabled;
    private static final ConfigValue<Boolean> httpWebsocketEnabled;
    private static final ConfigValue<List<? extends UnmodifiableConfig>> httpRules;

    private static final ConfigValue<Integer> httpTimeout;
    private static final ConfigValue<Integer> httpMaxRequests;
    private static final ConfigValue<Integer> httpMaxDownload;
    private static final ConfigValue<Integer> httpMaxUpload;
    private static final ConfigValue<Integer> httpMaxWebsockets;
    private static final ConfigValue<Integer> httpMaxWebsocketMessage;

    private static final ConfigValue<Boolean> commandBlockEnabled;
    private static final ConfigValue<Integer> modemRange;
    private static final ConfigValue<Integer> modemHighAltitudeRange;
    private static final ConfigValue<Integer> modemRangeDuringStorm;
    private static final ConfigValue<Integer> modemHighAltitudeRangeDuringStorm;
    private static final ConfigValue<Integer> maxNotesPerTick;

    private static final ConfigValue<Boolean> turtlesNeedFuel;
    private static final ConfigValue<Integer> turtleFuelLimit;
    private static final ConfigValue<Integer> advancedTurtleFuelLimit;
    private static final ConfigValue<Boolean> turtlesObeyBlockProtection;
    private static final ConfigValue<Boolean> turtlesCanPush;
    private static final ConfigValue<List<? extends String>> turtleDisabledActions;

    private static final ConfigValue<MonitorRenderer> monitorRenderer;

    private static final ForgeConfigSpec commonSpec;
    private static final ForgeConfigSpec clientSpec;

    private Config() {}

    static
    {
        Builder builder = new Builder();

        { // General computers
            computerSpaceLimit = builder
                .comment( "The disk space limit for computers and turtles, in bytes" )
                .translation( TRANSLATION_PREFIX + "computer_space_limit" )
                .define( "computer_space_limit", ComputerCraft.computerSpaceLimit );

            floppySpaceLimit = builder
                .comment( "The disk space limit for floppy disks, in bytes" )
                .translation( TRANSLATION_PREFIX + "floppy_space_limit" )
                .define( "floppy_space_limit", ComputerCraft.floppySpaceLimit );

            maximumFilesOpen = builder
                .comment( "Set how many files a computer can have open at the same time. Set to 0 for unlimited." )
                .translation( TRANSLATION_PREFIX + "maximum_open_files" )
                .defineInRange( "maximum_open_files", ComputerCraft.maximumFilesOpen, 0, Integer.MAX_VALUE );

            disableLua51Features = builder
                .comment( "Set this to true to disable Lua 5.1 functions that will be removed in a future update. " +
                    "Useful for ensuring forward compatibility of your programs now." )
                .define( "disable_lua51_features", ComputerCraft.disable_lua51_features );

            defaultComputerSettings = builder
                .comment( "A comma separated list of default system settings to set on new computers. Example: " +
                    "\"shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false\" will disable all " +
                    "autocompletion" )
                .define( "default_computer_settings", ComputerCraft.default_computer_settings );

            debugEnabled = builder
                .comment( "Enable Lua's debug library. This is sandboxed to each computer, so is generally safe to be used by players." )
                .define( "debug_enabled", ComputerCraft.debug_enable );

            logComputerErrors = builder
                .comment( "Log exceptions thrown by peripherals and other Lua objects.\n" +
                    "This makes it easier for mod authors to debug problems, but may result in log spam should people use buggy methods." )
                .define( "log_computer_errors", ComputerCraft.logPeripheralErrors );
        }

        {
            builder.comment( "Controls execution behaviour of computers. This is largely intended for fine-tuning " +
                "servers, and generally shouldn't need to be touched" );
            builder.push( "execution" );

            computerThreads = builder
                .comment( "Set the number of threads computers can run on. A higher number means more computers can run " +
                    "at once, but may induce lag.\n" +
                    "Please note that some mods may not work with a thread count higher than 1. Use with caution." )
                .worldRestart()
                .defineInRange( "computer_threads", ComputerCraft.computer_threads, 1, Integer.MAX_VALUE );

            maxMainGlobalTime = builder
                .comment( "The maximum time that can be spent executing tasks in a single tick, in milliseconds.\n" +
                    "Note, we will quite possibly go over this limit, as there's no way to tell how long a will take " +
                    "- this aims to be the upper bound of the average time." )
                .defineInRange( "max_main_global_time", (int) TimeUnit.NANOSECONDS.toMillis( ComputerCraft.maxMainGlobalTime ), 1, Integer.MAX_VALUE );

            maxMainComputerTime = builder
                .comment( "The ideal maximum time a computer can execute for in a tick, in milliseconds.\n" +
                    "Note, we will quite possibly go over this limit, as there's no way to tell how long a will take " +
                    "- this aims to be the upper bound of the average time." )
                .defineInRange( "max_main_computer_time", (int) TimeUnit.NANOSECONDS.toMillis( ComputerCraft.maxMainComputerTime ), 1, Integer.MAX_VALUE );

            builder.pop();
        }

        { // HTTP
            builder.comment( "Controls the HTTP API" );
            builder.push( "http" );

            httpEnabled = builder
                .comment( "Enable the \"http\" API on Computers (see \"rules\" for more fine grained control than this)." )
                .define( "enabled", ComputerCraft.httpEnabled );

            httpWebsocketEnabled = builder
                .comment( "Enable use of http websockets. This requires the \"http_enable\" option to also be true." )
                .define( "websocket_enabled", ComputerCraft.httpWebsocketEnabled );

            httpRules = builder
                .comment( "A list of rules which control which domains or IPs are allowed through the \"http\" API on computers.\n" +
                    "Each rule is an item with a 'host' to match against, and an action. " +
                    "The host may be a domain name (\"pastebin.com\"),\n" +
                    "wildcard (\"*.pastebin.com\") or CIDR notation (\"127.0.0.0/8\"). 'action' maybe 'allow' or 'block'. If no rules" +
                    "match, the domain will be blocked." )
                .defineList( "rules",
                    Stream.concat(
                        Stream.of( ComputerCraft.DEFAULT_HTTP_DENY ).map( x -> makeRule( x, "deny" ) ),
                        Stream.of( ComputerCraft.DEFAULT_HTTP_ALLOW ).map( x -> makeRule( x, "allow" ) )
                    ).collect( Collectors.toList() ),
                    x -> x instanceof UnmodifiableConfig && parseRule( (UnmodifiableConfig) x ) != null );

            httpTimeout = builder
                .comment( "The period of time (in milliseconds) to wait before a HTTP request times out. Set to 0 for unlimited." )
                .defineInRange( "timeout", ComputerCraft.httpTimeout, 0, Integer.MAX_VALUE );

            httpMaxRequests = builder
                .comment( "The number of http requests a computer can make at one time. Additional requests will be queued, and sent when the running requests have finished. Set to 0 for unlimited." )
                .defineInRange( "max_requests", ComputerCraft.httpMaxRequests, 0, Integer.MAX_VALUE );

            httpMaxDownload = builder
                .comment( "The maximum size (in bytes) that a computer can download in a single request. Note that responses may receive more data than allowed, but this data will not be returned to the client." )
                .defineInRange( "max_download", (int) ComputerCraft.httpMaxDownload, 0, Integer.MAX_VALUE );

            httpMaxUpload = builder
                .comment( "The maximum size (in bytes) that a computer can upload in a single request. This includes headers and POST text." )
                .defineInRange( "max_upload", (int) ComputerCraft.httpMaxUpload, 0, Integer.MAX_VALUE );

            httpMaxWebsockets = builder
                .comment( "The number of websockets a computer can have open at one time. Set to 0 for unlimited." )
                .defineInRange( "max_websockets", ComputerCraft.httpMaxWebsockets, 1, Integer.MAX_VALUE );

            httpMaxWebsocketMessage = builder
                .comment( "The maximum size (in bytes) that a computer can send or receive in one websocket packet." )
                .defineInRange( "max_websocket_message", ComputerCraft.httpMaxWebsocketMessage, 0, Websocket.MAX_MESSAGE_SIZE );

            builder.pop();
        }

        { // Peripherals
            builder.comment( "Various options relating to peripherals." );
            builder.push( "peripheral" );

            commandBlockEnabled = builder
                .comment( "Enable Command Block peripheral support" )
                .define( "command_block_enabled", ComputerCraft.enableCommandBlock );

            modemRange = builder
                .comment( "The range of Wireless Modems at low altitude in clear weather, in meters" )
                .defineInRange( "modem_range", ComputerCraft.modem_range, 0, MODEM_MAX_RANGE );

            modemHighAltitudeRange = builder
                .comment( "The range of Wireless Modems at maximum altitude in clear weather, in meters" )
                .defineInRange( "modem_high_altitude_range", ComputerCraft.modem_highAltitudeRange, 0, MODEM_MAX_RANGE );

            modemRangeDuringStorm = builder
                .comment( "The range of Wireless Modems at low altitude in stormy weather, in meters" )
                .defineInRange( "modem_range_during_storm", ComputerCraft.modem_rangeDuringStorm, 0, MODEM_MAX_RANGE );

            modemHighAltitudeRangeDuringStorm = builder
                .comment( "The range of Wireless Modems at maximum altitude in stormy weather, in meters" )
                .defineInRange( "modem_high_altitude_range_during_storm", ComputerCraft.modem_highAltitudeRangeDuringStorm, 0, MODEM_MAX_RANGE );

            maxNotesPerTick = builder
                .comment( "Maximum amount of notes a speaker can play at once" )
                .defineInRange( "max_notes_per_tick", ComputerCraft.maxNotesPerTick, 1, Integer.MAX_VALUE );

            builder.pop();
        }

        { // Turtles
            builder.comment( "Various options relating to turtles." );
            builder.push( "turtle" );

            turtlesNeedFuel = builder
                .comment( "Set whether Turtles require fuel to move" )
                .define( "need_fuel", ComputerCraft.turtlesNeedFuel );

            turtleFuelLimit = builder
                .comment( "The fuel limit for Turtles" )
                .defineInRange( "normal_fuel_limit", ComputerCraft.turtleFuelLimit, 0, Integer.MAX_VALUE );

            advancedTurtleFuelLimit = builder
                .comment( "The fuel limit for Advanced Turtles" )
                .defineInRange( "advanced_fuel_limit", ComputerCraft.advancedTurtleFuelLimit, 0, Integer.MAX_VALUE );

            turtlesObeyBlockProtection = builder
                .comment( "If set to true, Turtles will be unable to build, dig, or enter protected areas (such as near the server spawn point)" )
                .define( "obey_block_protection", ComputerCraft.turtlesObeyBlockProtection );

            turtlesCanPush = builder
                .comment( "If set to true, Turtles will push entities out of the way instead of stopping if there is space to do so" )
                .define( "can_push", ComputerCraft.turtlesCanPush );

            turtleDisabledActions = builder
                .comment( "A list of turtle actions which are disabled." )
                .defineList( "disabled_actions", Collections.emptyList(), x -> x instanceof String && getAction( (String) x ) != null );

            builder.pop();
        }

        commonSpec = builder.build();

        Builder clientBuilder = new Builder();
        monitorRenderer = clientBuilder
            .comment( "The renderer to use for monitors. Generally this should be kept at \"best\" - if " +
                "monitors have performance issues, you may wish to experiment with alternative renderers." )
            .defineEnum( "monitor_renderer", MonitorRenderer.BEST );
        clientSpec = clientBuilder.build();
    }

    public static void load()
    {
        ModLoadingContext.get().registerConfig( ModConfig.Type.COMMON, commonSpec );
        ModLoadingContext.get().registerConfig( ModConfig.Type.CLIENT, clientSpec );
    }

    public static void sync()
    {
        // General
        ComputerCraft.computerSpaceLimit = computerSpaceLimit.get();
        ComputerCraft.floppySpaceLimit = floppySpaceLimit.get();
        ComputerCraft.maximumFilesOpen = maximumFilesOpen.get();
        ComputerCraft.disable_lua51_features = disableLua51Features.get();
        ComputerCraft.default_computer_settings = defaultComputerSettings.get();
        ComputerCraft.debug_enable = debugEnabled.get();
        ComputerCraft.computer_threads = computerThreads.get();
        ComputerCraft.logPeripheralErrors = logComputerErrors.get();

        // Execution
        ComputerCraft.computer_threads = computerThreads.get();
        ComputerCraft.maxMainGlobalTime = TimeUnit.MILLISECONDS.toNanos( maxMainGlobalTime.get() );
        ComputerCraft.maxMainComputerTime = TimeUnit.MILLISECONDS.toNanos( maxMainComputerTime.get() );

        // HTTP
        ComputerCraft.httpEnabled = httpEnabled.get();
        ComputerCraft.httpWebsocketEnabled = httpWebsocketEnabled.get();
        ComputerCraft.httpRules = Collections.unmodifiableList( httpRules.get().stream()
            .map( Config::parseRule ).filter( Objects::nonNull ).collect( Collectors.toList() ) );

        ComputerCraft.httpTimeout = httpTimeout.get();
        ComputerCraft.httpMaxRequests = httpMaxRequests.get();
        ComputerCraft.httpMaxDownload = httpMaxDownload.get();
        ComputerCraft.httpMaxUpload = httpMaxUpload.get();
        ComputerCraft.httpMaxWebsockets = httpMaxWebsockets.get();
        ComputerCraft.httpMaxWebsocketMessage = httpMaxWebsocketMessage.get();

        // Peripheral
        ComputerCraft.enableCommandBlock = commandBlockEnabled.get();
        ComputerCraft.maxNotesPerTick = maxNotesPerTick.get();
        ComputerCraft.modem_range = modemRange.get();
        ComputerCraft.modem_highAltitudeRange = modemHighAltitudeRange.get();
        ComputerCraft.modem_rangeDuringStorm = modemRangeDuringStorm.get();
        ComputerCraft.modem_highAltitudeRangeDuringStorm = modemHighAltitudeRangeDuringStorm.get();

        // Turtles
        ComputerCraft.turtlesNeedFuel = turtlesNeedFuel.get();
        ComputerCraft.turtleFuelLimit = turtleFuelLimit.get();
        ComputerCraft.advancedTurtleFuelLimit = advancedTurtleFuelLimit.get();
        ComputerCraft.turtlesObeyBlockProtection = turtlesObeyBlockProtection.get();
        ComputerCraft.turtlesCanPush = turtlesCanPush.get();

        ComputerCraft.turtleDisabledActions.clear();
        for( String value : turtleDisabledActions.get() ) ComputerCraft.turtleDisabledActions.add( getAction( value ) );

        // Client
        ComputerCraft.monitorRenderer = monitorRenderer.get();
    }

    @SubscribeEvent
    public static void sync( ModConfig.Loading event )
    {
        sync();
    }

    @SubscribeEvent
    public static void sync( ModConfig.Reloading event )
    {
        // Ensure file configs are reloaded. Forge should probably do this, so worth checking in the future.
        CommentedConfig config = event.getConfig().getConfigData();
        if( config instanceof CommentedFileConfig ) ((CommentedFileConfig) config).load();

        sync();
    }

    private static final Converter<String, String> converter = CaseFormat.LOWER_CAMEL.converterTo( CaseFormat.UPPER_UNDERSCORE );

    private static TurtleAction getAction( String value )
    {
        try
        {
            return TurtleAction.valueOf( converter.convert( value ) );
        }
        catch( IllegalArgumentException e )
        {
            return null;
        }
    }

    private static UnmodifiableConfig makeRule( String host, String action )
    {
        com.electronwill.nightconfig.core.Config config = com.electronwill.nightconfig.core.Config.inMemory();
        config.add( "host", host );
        config.add( "action", action );
        return config;
    }

    @Nullable
    private static AddressRule parseRule( UnmodifiableConfig builder )
    {
        Object hostObj = builder.get( "host" );
        Object actionObj = builder.get( "action" );
        if( !(hostObj instanceof String) || !(actionObj instanceof String) ) return null;

        String host = (String) hostObj, action = (String) actionObj;
        for( AddressRule.Action candiate : AddressRule.Action.values() )
        {
            if( candiate.name().equalsIgnoreCase( action ) ) return AddressRule.parse( host, candiate );
        }

        return null;
    }
}
