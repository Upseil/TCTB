package com.upseil.game.scene2d;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.alpha;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.delay;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.fadeIn;
import static com.badlogic.gdx.scenes.scene2d.actions.Actions.moveTo;
import static com.upseil.game.Config.MenuConfigValues.AdditionalOutlinesFadeInDelay;
import static com.upseil.game.Config.MenuConfigValues.BackgroundAlpha;
import static com.upseil.game.Config.MenuConfigValues.BackgroundBlurFadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.BackgroundFadeOutDuration;
import static com.upseil.game.Config.MenuConfigValues.FillingsFadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.FlyInDelay;
import static com.upseil.game.Config.MenuConfigValues.FlyInDuration;
import static com.upseil.game.Config.MenuConfigValues.LogoAnimation;
import static com.upseil.game.Config.MenuConfigValues.LogoMoveDuration;
import static com.upseil.game.Config.MenuConfigValues.MaxAdditionalFillingsFadeInDelay;
import static com.upseil.game.Config.MenuConfigValues.OutlinesFadeInDuration;
import static com.upseil.game.Config.MenuConfigValues.ShadowFadeInDuration;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.upseil.game.Config.MenuConfig;
import com.upseil.game.math.Swing2Out;

public class LogoGroup extends WidgetGroup implements Disposable {
    
    private final LogoGroup.AnimationStyle animationStyle;
    private final TextureAtlas atlas;
    private final ObjectMap<String, AtlasRegion> regionMap;

    private final int width;
    private final int height;
    
    private float highestAdditionalFillingsFadeInDelay;
    private boolean hasOutlines;
    
    public LogoGroup(float worldWidth, LogoGroup.AnimationStyle animationStyle) {
        this.animationStyle = animationStyle;
        atlas = new TextureAtlas("title/title.atlas");
        this.width = atlas.getRegions().get(0).originalWidth;
        this.height = atlas.getRegions().get(0).originalHeight;
        
        regionMap = new ObjectMap<>();
        for (AtlasRegion region : atlas.getRegions()) {
            regionMap.put(region.name, region);
            if (region.name.startsWith("filling-")) {
                float additionalDelay = calculateAdditionalFillingsFadeInDelay(region);
                if (additionalDelay > highestAdditionalFillingsFadeInDelay) {
                    highestAdditionalFillingsFadeInDelay = additionalDelay;
                }
            }
            
            hasOutlines |= region.name.startsWith("outline");
        }
        
        addBackground(worldWidth);
        addFillings();
        addOutlines();
    }
    
    private void addBackground(float worldWidth) {
        addBackgroundBlur();

        AtlasRegion backgroundWhiteRegion = regionMap.get("background-white");
        AtlasRegion backgroundBlackRegion = regionMap.get("background-black");

        Image backgroundWhite = createAlignedImage(backgroundWhiteRegion);
        addActor(backgroundWhite);
        Image backgroundBlack = createAlignedImage(backgroundBlackRegion);
        addActor(backgroundBlack);
        
        float slope = (float) height / (backgroundWhiteRegion.packedWidth + backgroundBlackRegion.packedWidth - width);
        float delay = animationStyle.flyInDelay;
        float duration = animationStyle.flyInDuration;
        Interpolation interpolation = new Swing2Out();
        
        float backgroundWhiteStartX = worldWidth * -1.25f;
        float backgroundWhiteStartY = slope * backgroundWhiteStartX;
        backgroundWhite.setPosition(backgroundWhiteStartX, backgroundWhiteStartY);
        backgroundWhite.addAction(
            delay(delay, moveTo(backgroundWhiteRegion.offsetX, backgroundWhiteRegion.offsetY, duration, interpolation))
        );
        
        float backgroundBlackStartX = backgroundWhiteStartX * -1 + backgroundBlackRegion.offsetX;
        float backgroundBlackStartY = -1 * backgroundWhiteStartY;
        backgroundBlack.setPosition(backgroundBlackStartX, backgroundBlackStartY);
        backgroundBlack.addAction(
            delay(delay, moveTo(backgroundBlackRegion.offsetX, backgroundBlackRegion.offsetY, duration, interpolation))
        );
        
        float afterEffectsDelay = getAnimationDuration() + animationStyle.afterEffectsDelay;
        float afterAlpha = animationStyle.backgroundAlpha;
        backgroundWhite.addAction(delay(afterEffectsDelay, alpha(afterAlpha, animationStyle.backgroundFadeOutDuration, Interpolation.fade)));
        backgroundBlack.addAction(delay(afterEffectsDelay, alpha(afterAlpha, animationStyle.backgroundFadeOutDuration, Interpolation.fade)));
    }

    private void addBackgroundBlur() {
        AtlasRegion region = regionMap.get("background-blur");
        if (region != null) {
            float afterEffectsDelay = getAnimationDuration() + animationStyle.afterEffectsDelay;
            Image backgroundBlur = createFadeInImage(region, afterEffectsDelay, animationStyle.backgroundBlurFadeInDuration);
            backgroundBlur.moveBy(
                (width - backgroundBlur.getPrefWidth()) / 2,
                (height - backgroundBlur.getPrefHeight()) / 2
            );
            addActor(backgroundBlur);
        }
    }
    
    private void addFillings() {
        float afterEffectsDelay = getAnimationDuration() + animationStyle.afterEffectsDelay;
        float fillingsDelay = animationStyle.flyInDelay + animationStyle.flyInDuration;
        if (hasOutlines) {
            fillingsDelay += animationStyle.additionalOutlinesFadeInDelay + animationStyle.outlinesFadeInDuration * 0.5f;
        }
        
        for (AtlasRegion fillingRegion : regionMap.values()) {
            if (fillingRegion.name.startsWith("filling-")) {
                String shadowName = "shadow" + fillingRegion.name.substring(fillingRegion.name.indexOf('-'));
                AtlasRegion shadowRegion = regionMap.get(shadowName);
                
                if (shadowRegion != null) {
                    addActor(createFadeInImage(shadowRegion, afterEffectsDelay, animationStyle.shadowFadeInDuration));
                }
                float totalFillingDelay = fillingsDelay + calculateAdditionalFillingsFadeInDelay(fillingRegion);
                addActor(createFadeInImage(fillingRegion, totalFillingDelay, animationStyle.fillingsFadeInDuration));
            }
        }
    }

    private void addOutlines() {
        AtlasRegion outlinesStayRegion = regionMap.get("outlines-stay");
        AtlasRegion outlinesColorfulRegion = regionMap.get("outlines-colorful");
        if (outlinesStayRegion != null && outlinesColorfulRegion != null) {
            float delay = animationStyle.flyInDelay + animationStyle.flyInDuration + animationStyle.additionalOutlinesFadeInDelay;
            float duration = animationStyle.outlinesFadeInDuration;
            Image outlinesStay = createFadeInImage(outlinesStayRegion, delay, duration);
            Image outlinesColorful = createFadeInImage(outlinesColorfulRegion, delay, duration);
            addActor(outlinesStay);
            addActor(outlinesColorful);
        }
    }
    
    private Image createFadeInImage(AtlasRegion region, float delay, float duration) {
        Image image = createAlignedImage(region);
        setupFadeInAction(image, delay, duration);
        return image;
    }

    private void setupFadeInAction(Image image, float delay, float duration) {
        image.getColor().a = 0;
        image.addAction(delay(delay, fadeIn(duration, Interpolation.fade)));
    }
    
    private Image createAlignedImage(AtlasRegion region) {
        Image image = new Image(region);
        image.setPosition(region.offsetX, region.offsetY);
        return image;
    }
    
    private float calculateAdditionalFillingsFadeInDelay(AtlasRegion region) {
        return (region.offsetX / width) * animationStyle.maxAdditionalFillingsFadeInDelay;
    }
    
    public float getAnimationDuration() {
        float totalFlyInDuration = animationStyle.flyInDelay + animationStyle.flyInDuration;
        float totalFillingsDuration = highestAdditionalFillingsFadeInDelay + animationStyle.fillingsFadeInDuration * 0.5f;
        float totalOutlinesDuration = 0;
        if (hasOutlines) {
            totalOutlinesDuration = animationStyle.additionalOutlinesFadeInDelay + animationStyle.outlinesFadeInDuration * 0.5f;
        }
        return totalFlyInDuration + totalOutlinesDuration + totalFillingsDuration;
    }

    @Override
    public float getPrefWidth() {
        validate();
        return width * getScaleX();
    }

    @Override
    public float getPrefHeight() {
        validate();
        return height * getScaleY();
    }

    @Override
    public float getMaxWidth() {
        return width;
    }

    @Override
    public float getMaxHeight() {
        return height;
    }

    @Override
    public void dispose() {
        atlas.dispose();
    }
    
    public static class AnimationStyle {
        
        public final float flyInDelay;
        public final float flyInDuration;
        
        public final float backgroundAlpha;
        public final float backgroundFadeOutDuration;
        
        public final float additionalOutlinesFadeInDelay;
        public final float outlinesFadeInDuration;
        public final float maxAdditionalFillingsFadeInDelay;
        public final float fillingsFadeInDuration;
        
        public final float afterEffectsDelay;
        public final float backgroundBlurFadeInDuration;
        public final float shadowFadeInDuration;
        
        public static LogoGroup.AnimationStyle from(MenuConfig config) {
            if (config.getBoolean(LogoAnimation)) {
                return new AnimationStyle(config.getFloat(FlyInDelay), config.getFloat(FlyInDuration), config.getFloat(BackgroundAlpha),
                        config.getFloat(BackgroundFadeOutDuration), config.getFloat(AdditionalOutlinesFadeInDelay), config.getFloat(OutlinesFadeInDuration),
                        config.getFloat(FillingsFadeInDuration), config.getFloat(MaxAdditionalFillingsFadeInDelay), config.getFloat(LogoMoveDuration),
                        config.getFloat(BackgroundBlurFadeInDuration), config.getFloat(ShadowFadeInDuration));
            } else {
                return new AnimationStyle(0, 0, config.getFloat(BackgroundAlpha), 0, 0, 0, 0, 0, 0, 0, 0);
            }
        }
        
        public AnimationStyle(float flyInDelay, float flyInDuration, float backgroundAlpha, float backgroundFadeOutDuration,
                float additionalOutlinesFadeInDelay, float outlinesFadeInDuration, float fillingsFadeInDuration, float maxAdditionalFillingsFadeInDelay,
                float afterEffectsDelay, float backgroundBlurFadeInDuration, float shadowFadeInDuration) {
            this.flyInDelay = flyInDelay;
            this.flyInDuration = flyInDuration;
            this.backgroundAlpha = backgroundAlpha;
            this.backgroundFadeOutDuration = backgroundFadeOutDuration;
            this.additionalOutlinesFadeInDelay = additionalOutlinesFadeInDelay;
            this.outlinesFadeInDuration = outlinesFadeInDuration;
            this.fillingsFadeInDuration = fillingsFadeInDuration;
            this.maxAdditionalFillingsFadeInDelay = maxAdditionalFillingsFadeInDelay;
            this.afterEffectsDelay = afterEffectsDelay;
            this.backgroundBlurFadeInDuration = backgroundBlurFadeInDuration;
            this.shadowFadeInDuration = shadowFadeInDuration;
        }
        
    }
    
}