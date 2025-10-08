# Orbital Railgun Forge 1.20.1

This project is a Forge 47.x (Minecraft 1.20.1) port of [Mishkis/orbital-railgun](https://github.com/Mishkis/orbital-railgun).
It targets Java 17 and uses Parchment mappings via the Librarian ForgeGradle plugin for development.

## Notable Port Changes

* **Rendering:** Satin has been removed. Post-processing is implemented using Forge's `PostChain` API and custom shader instances that mirror the original Fabric effects. The chains reload and resize correctly and expose the same uniforms that the Fabric version populated.
* **Client state:** Charging, cooldown, strike data, and hit information are tracked by `RailgunState`. Forge events update the state each tick, keep the FOV stable while charging, and trigger shader updates during world and GUI rendering.
* **Networking:** Vanilla networking calls were replaced with Forge's `SimpleChannel`. Clients request a strike via `C2S_RequestFire`, the server validates the request, triggers `OrbitalRailgunStrikeManager`, and notifies nearby players with `S2C_PlayStrikeEffects`.
* **Strike manager:** Server-side strike logic mirrors the Fabric behaviour, including entity knockback, delayed detonation, damage, and block removal using the original radius mask.
* **Assets:** Shader programs, post chain JSON, Geckolib models, language strings, recipes, and damage types were migrated verbatim to the Forge resource layout.
* **FTB Chunks compatibility:** When FTB Chunks is installed, the railgun respects chunk claims with configurable permissions for explosions, block breaking, and entity damage.

## Development

* Java 17 toolchain (configured via Gradle).
* Forge 47.4.0 userdev + Librarian plugin (`parchment-2023.09.03-1.20.1`).
* Geckolib 4.2.2 is bundled for the item renderer.
* The Gradle wrapper scripts and binaries are intentionally omitted; invoke your local Gradle installation or generate a wrapper
  locally before building.

### Running the client

```
gradle runClient
```

### Building

```
gradle build
```

## Configuration

The common configuration file (`orbital-railgun-forge.toml`) now includes claim-protection toggles:

* `respectClaims` (default `true`): Enables FTB Chunks protection when the mod is available.
* `allowEntityDamageInClaims` (default `false`): Allows entity damage inside claims only when the shooter has permission.
* `allowBlockBreakInClaims` (default `false`): Allows block destruction inside claims only when the shooter has permission.
* `allowExplosionsInClaims` (default `false`): Allows explosions inside claims only when the shooter has permission.
* `opsBypassClaims` (default `true`): Lets operators bypass claim checks.

