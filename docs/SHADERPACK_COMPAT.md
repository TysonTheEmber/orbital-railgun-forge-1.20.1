# Orbital Railgun Shader Pack Compatibility

The Orbital Railgun now exposes two rendering paths that automatically swap depending on whether a shader pack (Iris/Oculus) is active.

## Rendering Paths

| Mode | Trigger | What happens |
|------|---------|--------------|
| **Vanilla Post** | No shader pack, or `compat.forceVanillaPostChain = true` | Original JSON/GLSL `PostChain` renders the strike visuals. |
| **Shader-Pack Geometry** | Iris/Oculus shader pack detected | Full-screen post-processing is skipped. Railgun effects render via custom geometry + Forge shaders and are consumed by the `OrbitalRailgun-Addon` shader pack. |

The mod re-evaluates shader pack state on resource reloads (`F3+T`) and logs the active path on first use. You will see additional messages when the add-on pack is missing or detected.

## Encoded Mask Data

The Forge geometry writes a 1×1 white mask texture while encoding gameplay state into vertex colors for the shader pack:

- **Red (`r`)** – Charge progress (0..1)
- **Green (`g`)** – Hit kind (0 = none, 0.5 = block, 1 = entity)
- **Blue (`b`)** – Normalised distance to target (~0..1)
- **Alpha (`a`)** – Intensity (charge or strike strength)

`gbuffers_translucent` captures this into an auxiliary render target that the `composite` and `final` passes read back for heat haze, chromatic wobble, and vignette flashes.

## Configuration Keys (`client.toml`)

| Key | Default | Description |
|-----|---------|-------------|
| `compat.overlayEnabled` | `true` | Draw a lightweight HUD flash in shader-pack mode. |
| `compat.forceVanillaPostChain` | `false` | Always run the vanilla `PostChain` even when a shader pack is active (not recommended). |
| `compat.logIrisState` | `true` | Log the Iris/Oculus detection result at startup and on reload. |

## Enabling the Shader-Pack Add-On

1. Copy `shaderpacks/OrbitalRailgun-Addon.zip` into your client’s `shaderpacks/` folder (or keep it alongside the repo when using a dev instance).
2. Launch Minecraft with Iris or Oculus.
3. In the shader pack selection screen, enable your main shader pack **and** add `OrbitalRailgun-Addon.zip` as an additional pack/layer.
4. Reload resources (`F3+T`) if you toggle packs while the game is running.

Without the add-on enabled, the beam geometry still renders, but the composite warp and flashes will be absent (a warning is logged).
