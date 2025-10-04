package net.tysontheember.orbitalrailgun.client.item;

import net.tysontheember.orbitalrailgun.ForgeOrbitalRailgunMod;
import net.tysontheember.orbitalrailgun.item.OrbitalRailgunItem;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class OrbitalRailgunRenderer extends GeoItemRenderer<OrbitalRailgunItem> {
    public OrbitalRailgunRenderer() {
        super(new DefaultedItemGeoModel<>(ForgeOrbitalRailgunMod.id("orbital_railgun")));
    }
}
