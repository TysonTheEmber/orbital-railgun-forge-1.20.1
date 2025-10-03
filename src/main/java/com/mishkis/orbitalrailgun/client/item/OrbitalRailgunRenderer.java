package com.mishkis.orbitalrailgun.client.item;

import com.mishkis.orbitalrailgun.ForgeOrbitalRailgunMod;
import com.mishkis.orbitalrailgun.item.OrbitalRailgunItem;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class OrbitalRailgunRenderer extends GeoItemRenderer<OrbitalRailgunItem> {
    public OrbitalRailgunRenderer() {
        super(new DefaultedItemGeoModel<>(ForgeOrbitalRailgunMod.id("orbital_railgun")));
    }
}
