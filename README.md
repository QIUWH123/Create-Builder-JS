# Create Builder JS

Create Builder JS (CBJS) lets modpack authors register and customize Create
blocks and equipment through KubeJS. It is an independent addon; CWI Core is
not required.

## Features

- Register cogwheels, shafts, casings, gearboxes, belts, conveyors, fluid devices,
  storage, logistics, generators, burners, goggles and wrenches.
- Customize textures, moving model parts, kinetic limits, storage dimensions,
  burner fuels and contraption restrictions for supported block families.
- React to kinetic, fluid, storage and belt changes, and customize goggles
  information with `CBJSEvents`.
- Connect opted-in cogwheels using chain, rope or belt drives and ride them,
  including without Bits 'n' Bobs or Azimuth.

## Usage

Download CBJS from [Releases](https://github.com/QIUWH123/Create-Builder-JS/releases)
and install it with its required dependencies on the client and server. Content is
defined by the pack's scripts; registering a block does not add a crafting recipe.
For example, put this in `kubejs/startup_scripts/` and restart:

```js
StartupEvents.registry('block', event => {
    event.create('example_cogwheel', 'cogwheel')
        .displayName('Example Cogwheel')
        .maxRpm(128)
        .chain().rope().belt()
})
```

Each drive method enables only its own material; all are off by default.
Without BnB, hold chain, string or a Create belt connector, right-click eligible
wheels in perimeter order, then click the first wheel again to close the loop.
Shift-click with the material to cancel selection. Hold a Create wrench in your
main hand and right-click the drive to ride; sneak to dismount. Shift-right-click
with the wrench to remove the loop and refund its paid materials.

Startup registration changes require a restart. Server-script kinetic rules and
burner fuels update with `/reload`. Pack-authored assets belong in `kubejs/assets/`.
The client commands `/cbjs resources validate` and
`/cbjs resources export <block_id>` check model references or export resource
templates for registered CBJS blocks.

## Requirements

- Minecraft **1.21.1**, **NeoForge 21.1.264–21.1.x**, and **Java 21**.
- Create **6.0.10–6.0.x**, with its required Flywheel, Ponder and Registrate stack.
- KubeJS **2101.7.2-build.377 or newer below 2101.8**, with its required Rhino.

Bits 'n' Bobs **2.3.5 or newer below 2.4** is optional and uses its own placement
controls when installed. ProbeJS is optional. Custom models need valid textures
supplied by the pack or its resource packs.

Standalone drives accept vanilla chain or CBJS-registered chain materials,
vanilla string and Create belt connectors. They require **2–16 exposed opted-in
CBJS wheels** with matching axes in a clear planar convex loop, within a
**64-block span**. Encased wheels cannot join. Arbitrary third-party material
items are not automatically supported. Carriages and non-planar routes remain
BnB features.

## License

Original CBJS code is licensed under the **GNU General Public License, version 3
only (GPL-3.0-only)**, not any later version. See [LICENSE](LICENSE) for the full
text. Bundled CBJS assets retain their MIT license; the Gradle wrapper and
referenced upstream resources retain their own licenses. See
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for these boundaries.
