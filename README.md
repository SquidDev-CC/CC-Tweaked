# ![CC: Tweaked](logo.png)
[![Build Status](https://travis-ci.org/SquidDev-CC/CC-Tweaked.svg?branch=master)](https://travis-ci.org/SquidDev-CC/CC-Tweaked)

CC: Tweaked is a fork of [ComputerCraft](https://github.com/dan200/ComputerCraft), adding programmable computers,
turtles and more to Minecraft.

## What?
ComputerCraft has always held a fond place in my heart: it's the mod which really got me into Minecraft, and it's the
mod which has kept me playing it for many years. However, development of the original mod has slowed in recent years,
as the original developers have had less time to work on the mod, and moved onto other projects and commitments 

CC:Tweaked (or CC:T for short) is an attempt to continue ComputerCraft's legacy. It's not intended to be a competitor
to CC, nor do I want to take it in a vastly different direction to the original mod. Instead, CC:T focuses on making the
ComputerCraft experience as _solid_ as possible, ironing out any wrinkles that may have developed over time.

## Features
CC: Tweaked contains all the features of the latest version of ComputerCraft, as well as numerous fixes, performance 
improvements and several nifty additions. I'd recommend checking out [the releases page](https://github.com/SquidDev-CC/CC-Tweaked/releases)
to see the full changes, but here's a couple of the more interesting changes:

 - Improvements to the `http` library, including websockets, support for other HTTP methods (`PUT`, `DELETE`, etc...) 
   and configurable limits on HTTP usage.
 - Full-block wired modems, allowing one to wrap non-solid peripherals (such as turtles, or chests if Plethora is
   installed).   
 - Pocket computers can be held like maps, allowing you to view the screen without entering a GUI.
 - Printed pages and books can be placed in item frames and held like maps.
 - Several profiling and administration tools for server owners, via the `/computercraft` command. This allows operators 
   to track which computers are hogging resources, turn on and shutdown multiple computers at once and interact with
   computers remotely.
 - Closer emulation of standard Lua, adding the `debug` and `io` libraries. This also enables seeking within binary
   files, meaning you don't need to read large files into memory.
 - Allow running multiple computers on multiple threads, reducing latency on worlds with many computers.

## Relation to CCTweaks?
This mod has nothing to do with CCTweaks, though there is no denying the name is a throwback to it. That being said,
several features have been included, such as full block modems, the Cobalt runtime and map-like rendering for pocket
computers.

## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. In order to start helping
develop CC:T, you'll need to follow these steps:

 - **Clone the repository:** `git clone https://github.com/SquidDev-CC/CC-Tweaked.git && cd CC-Tweaked`
 - **Setup Forge:** `./gradlew setupDecompWorkspace`
 - **Test your changes:** `./gradlew runClient` (or run the `GradleStart` class from your IDE).

If you want to run CC:T in a normal Minecraft instance, run `./gradlew build` and copy the `.jar` from `build/libs`.
