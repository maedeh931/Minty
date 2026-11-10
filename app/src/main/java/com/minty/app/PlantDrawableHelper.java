package com.minty.app;

import androidx.annotation.DrawableRes;

/**
 * Utility helper that resolves graphical vector drawables for all plant species across their 5 growth stages.
 */
public class PlantDrawableHelper {

    @DrawableRes
    public static int getPlantDrawable(String species, int stage) {
        switch (stage) {
            case GardenPlant.STAGE_SEED:
                return R.drawable.ic_plant_stage_seed;
            case GardenPlant.STAGE_SPROUT:
                return R.drawable.ic_plant_stage_sprout;
            case GardenPlant.STAGE_SEEDLING:
                return R.drawable.ic_plant_stage_seedling;
            case GardenPlant.STAGE_GROWING:
                return R.drawable.ic_plant_stage_growing;
            case GardenPlant.STAGE_BLOOMING:
            default:
                return getBloomedFlowerDrawable(species);
        }
    }

    @DrawableRes
    public static int getBloomedFlowerDrawable(String species) {
        if (species == null) return R.drawable.ic_flower_mint;
        switch (species.toLowerCase()) {
            case GardenPlant.SPECIES_SUNFLOWER:
                return R.drawable.ic_flower_sunflower;
            case GardenPlant.SPECIES_DAISY:
                return R.drawable.ic_flower_daisy;
            case GardenPlant.SPECIES_TULIP:
                return R.drawable.ic_flower_tulip;
            case GardenPlant.SPECIES_ROSE:
                return R.drawable.ic_flower_rose;
            case GardenPlant.SPECIES_LAVENDER:
                return R.drawable.ic_flower_lavender;
            case GardenPlant.SPECIES_CHERRY_BLOSSOM:
                return R.drawable.ic_flower_cherry;
            case GardenPlant.SPECIES_MINT:
            default:
                return R.drawable.ic_flower_mint;
        }
    }
}
