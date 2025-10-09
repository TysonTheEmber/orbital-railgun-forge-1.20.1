# Orbital Railgun Forge 1.20.1

This project is a Forge 47.x (Minecraft 1.20.1) port of [Mishkis/orbital-railgun](https://github.com/Mishkis/orbital-railgun).
It targets Java 17 and uses Parchment mappings via the Librarian ForgeGradle plugin for development.

## Notable Port Changes

* **Rendering:** Satin has been removed. Post-processing is implemented using Forge's `PostChain` API and custom shader instances that mirror the original Fabric effects. The chains reload and resize correctly and expose the same uniforms that the Fabric version populated.
* **Client state:** Charging, cooldown, strike data, and hit information are tracked by `RailgunState`. Forge events update the state each tick, keep the FOV stable while charging, and trigger shader updates during world and GUI rendering.
* **Networking:** Vanilla networking calls were replaced with Forge's `SimpleChannel`. Clients request a strike via `C2S_RequestFire`, the server validates the request, triggers `OrbitalRailgunStrikeManager`, and notifies nearby players with `S2C_PlayStrikeEffects`.
* **Strike manager:** Server-side strike logic mirrors the Fabric behaviour, including entity knockback, delayed detonation, damage, and block removal using the original radius mask.
* **Assets:** Shader programs, post chain JSON, Geckolib models, language strings, recipes, and damage types were migrated verbatim to the Forge resource layout.
* **Claims compatibility:** When FTB Chunks or Open Parties & Claims are installed the railgun respects land claims, with configurable rules for explosions, block breaking, and entity damage.

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

Common configuration values live in `config/orbital-railgun-common.toml`. In addition to damage, cooldown, and blacklist settings the mod exposes claim-protection toggles:

* `respectClaims` — master switch enabling FTB Chunks claim protection.
* `respectOPACClaims` — toggle Open Parties & Claims protection when the mod is present.
* `allowEntityDamageInClaims` — allow entity damage in claims when the shooter has permission.
* `allowBlockBreakInClaims` — allow block destruction in claims when the shooter has permission.
* `allowExplosionsInClaims` — allow the explosion effect in claims when the shooter has permission.
* `opsBypassClaims` — let server operators bypass claim checks.

The orbital strike diameter can also be tuned:

* `destructionDiameter` (common) — diameter in blocks used for the strike's destructive physics. The server enforces this value.
* `shaderDiameter` (client) — visual-only diameter used by the post-processing shaders. Leave `syncShaderDiameterWithServer` enabled to let strike packets temporarily override the visuals with the server's value during impacts.

## Manual testing

To validate the operator strike command on a local dedicated server:

1. Start the integrated server with `gradle runServer` and join the world as an operator.
2. Run `/orbitalstrike 100 -200` and confirm the strike spools up at the surface Y of (100, ?, -200).
3. Run `/orbitalstrike 0 0 2.5 6` to verify the optional power and radius parameters schedule a wider blast.
4. Demote your player or join as a non-op account and confirm the command is rejected with a permission error.

