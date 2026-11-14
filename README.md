# Create Builder JS

An independent Create and KubeJS addon for registering machinery, storage,
fluid devices, logistics and equipment. Mod ID: `cbjs`. This branch targets
Minecraft 1.21.1, NeoForge and Java 21.

## Requirements

- Minecraft 1.21.1 and NeoForge 21.1.264 or newer within the 21.1 line.
- Create 6.0.10 within the 6.0 line, with its required supporting libraries.
- KubeJS 2101.7.2-build.377 or newer within the 2101.7 line.

Bits 'n' Bobs is optional. Chain, rope and belt drives and riding also work
without it or Azimuth. Cogwheel registrations independently opt in using
`.chain()`, `.rope()` and `.belt()`. Standalone routes are planar convex loops
of 2–16 exposed wheels, with a maximum 64-block span. Carriages and non-planar
routes require the optional BnB integration.

## Building

Use Java 21 and the included Gradle wrapper. Development dependencies remain
separate downloads and are never included in this repository or the mod JAR.
Place the pinned JARs listed in `gradle/development-dependencies.json` in their
listed `libs/` directories before building. Obtain them from their respective
projects; the manifest records SHA-256 checksums so the inputs can be verified.
All listed compile dependencies are needed to compile optional integration
classes even when those integrations are not loaded at runtime.

```sh
./gradlew check build
```

The distribution is `build/libs/cbjs-<version>.jar`. NeoForge development runs
include `runClient`, `runServer` and `runGameTestServer`; optional development
integrations can be enabled with `-PdevIntegrations`. `-PofflineTarget` is a
private diagnostic path and cannot produce distribution JARs.

Versioned releases attach the verified distribution JAR and its SHA-256 checksum
to GitHub Releases. Minecraft branches and tags identify the compatibility line;
the mod's version and JAR filename contain its own version.

## Licensing

CBJS is MIT licensed; see `LICENSE`. Referenced Create/Minecraft resources and
separately downloaded dependencies retain their upstream licenses. Gradle's
wrapper files retain their embedded upstream notices. Pack-specific artwork,
registrations, private documentation, evidence and runtime files are excluded.
