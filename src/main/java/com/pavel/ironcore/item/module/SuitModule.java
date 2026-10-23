package com.pavel.ironcore.item.module;

/**
 * Модули костюма (Gantry Systems из DESIGN_V2). Ставятся в нагрудник через Suit Station,
 * работают от энергии импланта. Эффекты — в {@link SuitModules#tick}.
 */
public enum SuitModule {
    THERMAL_SHIELD("thermal_shield", Category.UTILITY, "THR"),
    COOLANT("coolant", Category.SUPPORT, "COOL"),
    REBREATHER("rebreather", Category.UTILITY, "AIR"),
    AUTO_REPAIR("auto_repair", Category.SUPPORT, "REP"),
    RADAR("radar", Category.UTILITY, "RAD"),
    MICRO_MISSILES("micro_missiles", Category.WEAPONRY, "MSL"),
    UNIBEAM("unibeam", Category.WEAPONRY, "UNI");

    public enum Category { WEAPONRY, UTILITY, SUPPORT }

    private final String id;
    private final Category category;
    private final String hudTag;

    SuitModule(String id, Category category, String hudTag) {
        this.id = id;
        this.category = category;
        this.hudTag = hudTag;
    }

    public String id() { return id; }
    public Category category() { return category; }
    /** Короткая метка для строки модулей в HUD. */
    public String hudTag() { return hudTag; }
    public String translationKey() { return "module.ironcore." + id; }
    public String descriptionKey() { return "module.ironcore." + id + ".desc"; }
}
