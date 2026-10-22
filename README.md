# Create Builder JS

Create Builder JS (CBJS) is a Create and KubeJS addon for registering machinery,
fluid devices, storage blocks and equipment from scripts. It also provides events
for kinetic networks, fluid flow, storage contents, belts and goggles information.

Mod ID: `cbjs`. Java package: `net.quiwh.cbjs`. Event group: `CBJSEvents`.

## Requirements

- Minecraft 1.20.1 and Forge 47.4.10
- Java 17
- Released Create 6.0.8 with its supporting libraries
- KubeJS 2001.6.5-build.26, Rhino 2001.2.3-build.10 and Architectury 9.2.14

Create Connected is not required. Bits 'n' Bobs 0.0.41 is optional for cogwheel
chain integration. ProbeJS, JEI and Jade are development helpers.

## Registration types

| Area | Builder types |
| --- | --- |
| Gears and shafts | `cogwheel`, `large_cogwheel`, `shaftless_cogwheel`, `large_shaftless_cogwheel`, `shaft` |
| Transmission | `gearbox`, `vertical_gearbox`, `six_way_gearbox`, `encased_chain_drive`, `clutch`, `gearshift`, `rotation_speed_controller` |
| Belts and chains | `mechanical_belt`, `chain` |
| Fluids | `fluid_pipe`, `mechanical_pump`, `smart_pipe`, `fluid_valve` |
| Storage | `fluid_tank`, `fluid_vessel`, `item_vault`, `item_silo` |
| Casings and items | `casing` block; `goggles` and `wrench` items |

Place block registrations in your modpack's `kubejs/startup_scripts/` directory.
For example:

```js
StartupEvents.registry('block', event => {
    event.create('example_cogwheel', 'cogwheel')
        .displayName('Example Cogwheel')
        .maxRpm(128)
        .maxStress(16384)
})
```

Unqualified IDs use the `kubejs` namespace. Registrations inherit ordinary KubeJS
block settings such as hardness, sound, tags and callbacks. Cogwheels generate
casing companions unless `.noEncasedVariants()` is used.

Use `.replaceTexture(role, path)` to replace texture slots, or `.model(path)` to
supply custom geometry where supported. Model and texture paths omit file
extensions. Optional named model parts allow separate moving components. Supply
your own `.model(...)` for a six-way gearbox, which does not include a built-in
body model.

`.maxRpm(int)` sets an explicit RPM cap; `.noMaxRPM()` removes it. Omitted RPM
limits follow the applicable Create policy. `.maxStress(int)` caps the connected
network's total stress demand in SU. No stress cap is added when omitted.

Storage dimensions and belt length use `.maxSize(int)`, `.maxHeight(int)` or
`.maxLength(int)` where applicable. Omitted settings follow Create defaults,
including live configuration values. The corresponding `.noMaxSize()`,
`.noMaxHeight()` and `.noMaxLength()` methods remove those caps.

## Events

Server events include:

- `kineticData`, `kineticSpeedChanged`, `kineticStressChanged`, `kineticOverload`
- `fluidPipeFlow`, `fluidPipeFlowChanged`, `fluidPipePressureChanged`, `fluidPipeCollision`
- `storageFluidChanged`, `storageInventoryChanged`, `storageFormed`, `storageResized`, `storageSplit`
- `beltItemEntered`, `beltItemExited`, `beltItemProcessed`
- `blazeBurnerFuel`

`gogglesTooltip` runs in client scripts. Kinetic limits can also be configured in
server scripts:

```js
CBJSEvents.kineticData(event => {
    event.modifyKineticData('create:shaft').maxRpm(128)
})
```

Startup registrations require a game restart. Server event and limit scripts
support `/reload`. ProbeJS provides completion for the builders and event API.

## Building

This repository contains the mod source, loader resources, source tests and
Gradle build configuration. Use Java 17.

The build expects these dependency JARs in an ignored `libs/` directory:

| Dependency | Filename in `libs/` |
| --- | --- |
| Create 6.0.8 | `create-1.20.1-6.0.8-slim.jar` |
| KubeJS | `kubejs-forge-2001.6.5-build.26.jar` |
| Rhino | `rhino-forge-2001.2.3-build.10.jar` |
| Architectury | `architectury-9.2.14.jar` |
| Bits 'n' Bobs | `bits_n_bobs-0.0.41.jar` |
| ProbeJS | `probejs-forge-6.0.1.jar` |
| JEI | `jei-1.20.1-forge-15.56.0.205.jar` |
| Jade | `Jade-1.20.1-Forge-11.13.3.jar` |

Prepare the slim Create JAR from the released Forge 6.0.8 JAR: copy it, remove
only `META-INF/jarjar/` and its contents from the copy, and use the filename above.
Keep all other entries, including loader metadata. Gradle supplies the supporting
libraries separately. Maven Create build `6.0.8-291` uses a different Flywheel API
and is incompatible with this setup. The other listed JARs can be copied or
renamed to the expected filenames without changing their contents.

```sh
./gradlew build
./gradlew testKineticLimits
```

Import the Gradle project in IDEA and run `./gradlew genIntellijRuns` to generate
development run configurations. `./gradlew cbjsDistribution` packages the mod JAR
and README. Forge GameTests that use scripted blocks require matching KubeJS
registrations and test structures in the development instance.

## Credits

[Create: Bits ’n’ Bobs](https://www.curseforge.com/minecraft/mc-mods/create-bits-n-bobs)
— MIT License.

Create Builder JS integrates custom cogwheels and chain materials with its
cogwheel chain system when Bits ’n’ Bobs is installed. Thanks to its contributors.
