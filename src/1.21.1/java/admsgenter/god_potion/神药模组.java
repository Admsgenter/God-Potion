package admsgenter.god_potion;

import com.google.common.base.Predicates;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod(神药模组.MODID)
@EventBusSubscriber
public class 神药模组 {
    public static final String MODID = "god_potion";
    private static ModConfigSpec.Builder 构建器 = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue 时长配置 = 构建器.comment("神药持续时间", "Duration of God Potion").defineInRange("duration", 72000, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.IntValue 粒子配置 = 构建器.comment("神药饮用时产生的粒子数", "The number of particles generated when drinking God Potion")
        .defineInRange("particle", 128, 0, Integer.MAX_VALUE);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> 白名单配置 = 构建器.comment("药水效果白名单", "Potion Effect Whitelist")
        .defineList("potion_effect_whitelist", List.of(
            MobEffects.DIG_SPEED.getKey().location().toString(),
            MobEffects.ABSORPTION.getKey().location().toString(),
            MobEffects.CONDUIT_POWER.getKey().location().toString(),
            MobEffects.DOLPHINS_GRACE.getKey().location().toString()
        ), Predicates.alwaysTrue());
    public static final ModConfigSpec.ConfigValue<List<? extends String>> 黑名单配置 = 构建器.comment("药水效果黑名单", "Potion Effect Blacklist")
        .defineList("potion_effect_blacklist", List.of(
            MobEffects.JUMP.getKey().location().toString(),
            MobEffects.INVISIBILITY.getKey().location().toString(),
            MobEffects.HEALTH_BOOST.getKey().location().toString(),
            MobEffects.SLOW_FALLING.getKey().location().toString()
        ), Predicates.alwaysTrue());
    public static final ModConfigSpec.BooleanValue 自定义配置 = 构建器.comment(
        "启用自定义药水效果。如果启用，配置 \"药水效果白名单\" 和 \"药水效果黑名单\" 将被忽略，神药的药水效果将被配置 \"自定义药水效果\" 替换。",
        "Use custom potion effects. If enabled, config \"Potion Effect Whitelist\" and \"Potion Effect Blacklist\" will be ignored, God Potion effects will be replaced by config \"Custom Potion Effects\""
    ).define(List.of("自定义效果 (Custom Effects)", "enabled"), false);
    public static final ModConfigSpec.ConfigValue<List<? extends String>> 效果配置 = 构建器.comment("自定义药水效果", "Custom Potion Effects")
        .defineList(List.of("自定义效果 (Custom Effects)", "custom_potion_effects"), Collections.emptyList(), Predicates.alwaysTrue());
    public static final Map<Holder<MobEffect>, ModConfigSpec.ConfigValue<Integer>> 等级配置 = new Reference2ReferenceOpenHashMap<>();
    public static final ObjectOpenHashSet<Holder<MobEffect>> 药水效果集 = new ObjectOpenHashSet<>();
    public static List<MobEffectInstance> 效果表 = new ReferenceArrayList<>();

    @SubscribeEvent
    public static void 构建(final BuildCreativeModeTabContentsEvent 事件) {
        if(CreativeModeTabs.FOOD_AND_DRINKS.equals(事件.getTabKey())) {
            事件.accept(神药物品.神药瓶);
            事件.accept(神药物品.神药水瓶);
            事件.accept(神药物品.神药);
        }
    }

    @SubscribeEvent
    public static void 加载(final FMLLoadCompleteEvent 事件) {
        Object2IntMap<Holder<MobEffect>> 等级表 = new Object2IntOpenHashMap<>(BuiltInRegistries.MOB_EFFECT.size());
        for(Holder<MobEffect> 效果 : BuiltInRegistries.MOB_EFFECT.asHolderIdMap()) 等级表.put(效果, 0);
        for(Potion 药水 : BuiltInRegistries.POTION) for(MobEffectInstance 效果 : 药水.getEffects()) 等级表.merge(药水效果集.addOrGet(效果.getEffect()), 效果.getAmplifier(), Math::max);
        for(Object2IntMap.Entry<Holder<MobEffect>> 等级对 : 等级表.object2IntEntrySet())
            等级配置.put(等级对.getKey(), 构建器.translation(等级对.getKey().value().getDescriptionId()).define(List.of("效果等级 (Effect Levels)",
                等级对.getKey().getKey().location().toString()), 等级对.getIntValue()));
        ModLoadingContext.get().getActiveContainer().registerConfig(ModConfig.Type.SERVER, 构建器.build());
        构建器 = null;
    }

    @SubscribeEvent @OnlyIn(Dist.CLIENT)
    public static void 颜色(final RegisterColorHandlersEvent.Item 事件) {
        事件.register((物品组, 层) -> 层 == 0? 0xFF385DC6:-1, 神药物品.神药水瓶);
        事件.register((物品组, 层) -> {
            if(层 == 0) {
                int R = (int) (System.currentTimeMillis()%1536)-768, G = R < 256? R+512:R-1024, B = R < -256? R+1024:R-512;
                return 0xFF000000|
                    Mth.clamp((R < 0? ~R:R)-0xFF, 0, 0xFF)<<16|
                    Mth.clamp((G < 0? ~G:G)-0xFF, 0, 0xFF)<<8|
                    Mth.clamp((B < 0? ~B:B)-0xFF, 0, 0xFF);
            } else return -1;
        }, 神药物品.神药);
    }

    private static List<Holder<MobEffect>> 效果表(ModConfigSpec.ConfigValue<List<? extends String>> 白名单配置) {
        return Lists.transform(白名单配置.get(), ID -> BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse(ID)).orElse(null));
    }

    @SubscribeEvent
    public static void 配置(final ModConfigEvent 事件) {
        效果表 = new ReferenceArrayList<>(BuiltInRegistries.MOB_EFFECT.size());
        if(!自定义配置.get()) {
            Set<Holder<MobEffect>> 白名单效果集 = Set.copyOf(效果表(白名单配置)), 黑名单效果集 = Set.copyOf(效果表(黑名单配置));
            for(Holder<MobEffect> 效果 : BuiltInRegistries.MOB_EFFECT.asHolderIdMap())
                if(白名单效果集.contains(效果) || !黑名单效果集.contains(效果) && 药水效果集.contains(效果) && 效果.value().isBeneficial() && !效果.value().isInstantenous())
                    效果表.add(new MobEffectInstance(效果, 时长配置.get(), 等级配置.get(效果).get()));
        } else for(Holder<MobEffect> 效果 : 效果表(效果配置)) if(效果 != null) 效果表.add(new MobEffectInstance(效果, 时长配置.get(), 等级配置.get(效果).get()));
        效果表 = List.copyOf(效果表);
    }

    @SubscribeEvent
    public static void 注册(final RegisterBrewingRecipesEvent 事件) { 事件.getBuilder().addRecipe(Ingredient.of(神药物品.神药水瓶), Ingredient.of(Items.NETHER_STAR), new ItemStack(神药物品.神药)); }

    @SubscribeEvent
    public static void 注册(final RegisterEvent 事件) {
        if(Registries.ITEM.equals(事件.getRegistryKey())) {
            事件.register(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "god_potion_bottle"), Suppliers.ofInstance(神药物品.神药瓶));
            事件.register(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "water_god_potion_bottle"), Suppliers.ofInstance(神药物品.神药水瓶));
            事件.register(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(MODID, "god_potion"), Suppliers.ofInstance(神药物品.神药));
        }
    }
}