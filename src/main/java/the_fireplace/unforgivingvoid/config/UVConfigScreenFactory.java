package the_fireplace.unforgivingvoid.config;

import dev.the_fireplace.lib.api.chat.injectables.TranslatorFactory;
import dev.the_fireplace.lib.api.chat.interfaces.Translator;
import dev.the_fireplace.lib.api.client.injectables.ConfigScreenBuilderFactory;
import dev.the_fireplace.lib.api.client.interfaces.ConfigScreenBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import the_fireplace.unforgivingvoid.UnforgivingVoidConstants;
import the_fireplace.unforgivingvoid.domain.config.DimensionSettings;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
@Singleton
public final class UVConfigScreenFactory {
    private static final String TRANSLATION_BASE = "text.config." + UnforgivingVoidConstants.MODID + ".";
    private static final String OPTION_TRANSLATION_BASE = TRANSLATION_BASE + "option.";

    private final Translator translator;
    private final ConfigScreenBuilderFactory configScreenBuilderFactory;
    private final DimensionConfigManager dimensionConfigManager;

    private final DimensionConfig fallbackDimensionConfig;
    private final DimensionSettings defaultFallbackSettings;

    private ConfigScreenBuilder configScreenBuilder;

    @Inject
    public UVConfigScreenFactory(
        TranslatorFactory translatorFactory,
        ConfigScreenBuilderFactory configScreenBuilderFactory,
        DimensionConfigManager dimensionConfigManager,
        FallbackDimensionConfig fallbackDimensionConfig,
        @Named("default") DimensionSettings defaultFallbackSettings
    ) {
        this.translator = translatorFactory.getTranslator(UnforgivingVoidConstants.MODID);
        this.configScreenBuilderFactory = configScreenBuilderFactory;
        this.dimensionConfigManager = dimensionConfigManager;
        this.fallbackDimensionConfig = fallbackDimensionConfig;
        this.defaultFallbackSettings = defaultFallbackSettings;
    }

    public Screen getConfigScreen(Screen parent) {
        this.configScreenBuilder = configScreenBuilderFactory.create(
            translator,
            TRANSLATION_BASE + "title",
            TRANSLATION_BASE + "default",
            parent,
            dimensionConfigManager::saveAll
        );

        buildDefaultDimensionConfigCategory(fallbackDimensionConfig);
        for (Identifier customMobId : dimensionConfigManager.getDimensionIdsWithCustomSettings()) {
            buildCustomDimensionConfigCategory(customMobId, dimensionConfigManager.getSettings(customMobId));
        }

        return this.configScreenBuilder.build();
    }

    private void addCommonDimensionConfigCategoryOptions(DimensionConfig dimensionConfig, DimensionSettings defaultSettings) {
        configScreenBuilder.addBoolToggle(
            OPTION_TRANSLATION_BASE + "isEnabled",
            dimensionConfig.isEnabled(),
            defaultSettings.isEnabled(),
            dimensionConfig::setEnabled,
            (byte) 0
        );
        configScreenBuilder.addByteField(
            OPTION_TRANSLATION_BASE + "triggerDistance",
            dimensionConfig.getTriggerDistance(),
            defaultSettings.getTriggerDistance(),
            dimensionConfig::setTriggerDistance,
            (byte) 1,
            Byte.MAX_VALUE
        );
        configScreenBuilder.addBoolToggle(
            OPTION_TRANSLATION_BASE + "dropObsidian",
            dimensionConfig.isDropObsidian(),
            defaultSettings.isDropObsidian(),
            dimensionConfig::setDropObsidian
        );
        configScreenBuilder.addIntField(
            OPTION_TRANSLATION_BASE + "fireResistanceSeconds",
            dimensionConfig.getFireResistanceSeconds(),
            defaultSettings.getFireResistanceSeconds(),
            dimensionConfig::setFireResistanceSeconds,
            0,
            Integer.MAX_VALUE
        );
        configScreenBuilder.addIntField(
            OPTION_TRANSLATION_BASE + "horizontalDistanceOffset",
            dimensionConfig.getHorizontalDistanceOffset(),
            defaultSettings.getHorizontalDistanceOffset(),
            dimensionConfig::setHorizontalDistanceOffset,
            0,
            Integer.MAX_VALUE
        );
        Set<String> dimensionIds = new HashSet<>();
        for (Identifier dimensionId : dimensionConfigManager.getDimensionIds()) {
            dimensionIds.add(dimensionId.toString());
        }
        configScreenBuilder.addStringDropdown(
            OPTION_TRANSLATION_BASE + "targetDimension",
            dimensionConfig.getTargetDimension(),
            defaultSettings.getTargetDimension(),
            dimensionIds,
            dimensionConfig::setTargetDimension,
            true
        );
        configScreenBuilder.addShortField(
            OPTION_TRANSLATION_BASE + "approximateSpawnY",
            dimensionConfig.getApproximateSpawnY(),
            defaultSettings.getApproximateSpawnY(),
            dimensionConfig::setApproximateSpawnY
        );
        configScreenBuilder.addBoolToggle(
            OPTION_TRANSLATION_BASE + "attemptFindSafePlatform",
            dimensionConfig.isAttemptFindSafePlatform(),
            defaultSettings.isAttemptFindSafePlatform(),
            dimensionConfig::setAttemptFindSafePlatform
        );
        configScreenBuilder.addBoolToggle(
            OPTION_TRANSLATION_BASE + "avoidSkySpawning",
            dimensionConfig.isAvoidSkySpawning(),
            defaultSettings.isAvoidSkySpawning(),
            dimensionConfig::setAvoidSkySpawning
        );
    }

    private void buildDefaultDimensionConfigCategory(DimensionConfig dimensionConfig) {
        addCommonDimensionConfigCategoryOptions(dimensionConfig, defaultFallbackSettings);
        createAddCustomDimensionDropdown();
    }

    private void createAddCustomDimensionDropdown() {
        // Not a real, savable option, but it's the easiest way to allow adding custom dimension settings in the GUI without hacking cloth config or building something custom that users won't be familiar with.
        configScreenBuilder.addStringDropdown(
            OPTION_TRANSLATION_BASE + "addCustomDimensionConfig",
            "",
            "",
            dimensionConfigManager.getDimensionIdsWithoutCustomSettings().stream().map(Identifier::toString).sorted().collect(Collectors.toList()),
            newValue -> {
                if (!newValue.isEmpty()) {
                    dimensionConfigManager.addCustom(new Identifier(newValue), fallbackDimensionConfig.clone());
                }
            },
            false,
            (byte) 2,
            value ->
                dimensionConfigManager.isCustom(new Identifier(value))
                    ? Optional.of(translator.getTranslatedText(OPTION_TRANSLATION_BASE + "addCustomDimensionConfig.err"))
                    : Optional.empty()
        );
    }

    private void buildCustomDimensionConfigCategory(Identifier identifier, DimensionConfig dimensionConfig) {
        addCommonDimensionConfigCategoryOptions(dimensionConfig, fallbackDimensionConfig);
        createRemoveCustomDimensionButton(identifier);
    }

    private void createRemoveCustomDimensionButton(Identifier identifier) {
        // Fake option, used to allow deleting custom settings without hacking cloth config
        configScreenBuilder.addBoolToggle(
            OPTION_TRANSLATION_BASE + "deleteCustomDimensionConfig",
            false,
            false,
            newValue -> {
                if (newValue) {
                    dimensionConfigManager.deleteCustom(identifier);
                }
            },
            (byte) 2
        );
    }
}