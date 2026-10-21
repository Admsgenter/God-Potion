package admsgenter.god_potion;

import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.RegisterEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mod(神药模组.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class 神药模组 {
    public static final String MODID = "god_potion";
    private static ForgeConfigSpec.Builder 构建器 = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue 时长配置 = 构建器.comment("神药持续时间", "Duration of God Potion").defineInRange("duration", 72000, 0, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.IntValue 粒子配置 = 构建器.comment("神药饮用时产生的粒子数", "The number of particles generated when drinking God Potion")
        .defineInRange("particle", 128, 0, Integer.MAX_VALUE);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> 白名单配置 = 构建器.comment("药水效果白名单", "Potion Effect Whitelist")
        .defineList("potion_effect_whitelist", List.of(
            Registry.MOB_EFFECT.getKey(MobEffects.DIG_SPEED).toString(),
            Registry.MOB_EFFECT.getKey(MobEffects.ABSORPTION).toString(),
            Registry.MOB_EFFECT.getKey(MobEffects.CONDUIT_POWER).toString(),
            Registry.MOB_EFFECT.getKey(MobEffects.DOLPHINS_GRACE).toString()
        ), Predicates.alwaysTrue());
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> 黑名单配置 = 构建器.comment("药水效果黑名单", "Potion Effect Blacklist")
        .defineList("potion_effect_blacklist", List.of(
            Registry.MOB_EFFECT.getKey(MobEffects.JUMP).toString(),
            Registry.MOB_EFFECT.getKey(MobEffects.INVISIBILITY).toString(),
            Registry.MOB_EFFECT.getKey(MobEffects.HEALTH_BOOST).toString(),
            Registry.MOB_EFFECT.getKey(MobEffects.SLOW_FALLING).toString()
        ), Predicates.alwaysTrue());
    public static final ForgeConfigSpec.BooleanValue 自定义配置 = 构建器.comment(
        "启用自定义药水效果。如果启用，配置 \"药水效果白名单\" 和 \"药水效果黑名单\" 将被忽略，神药的药水效果将被配置 \"自定义药水效果\" 替换。",
        "Use custom potion effects. If enabled, config \"Potion Effect Whitelist\" and \"Potion Effect Blacklist\" will be ignored, God Potion effects will be replaced by config \"Custom Potion Effects\""
    ).define(List.of("自定义效果 (Custom Effects)", "enabled"), false);
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> 效果配置 = 构建器.comment("自定义药水效果", "Custom Potion Effects")
        .defineList(List.of("自定义效果 (Custom Effects)", "custom_potion_effects"), Collections.emptyList(), Predicates.alwaysTrue());
    public static final ItemStack 效果物品组 = new ItemStack(Items.AIR);
    public static final Map<MobEffect, ForgeConfigSpec.ConfigValue<Integer>> 等级配置 = new Reference2ReferenceOpenHashMap<>();
    public static final ObjectOpenHashSet<MobEffect> 药水效果集 = new ObjectOpenHashSet<>();
    public static List<MobEffectInstance> 效果表 = Collections.emptyList();

    @SubscribeEvent
    public static void 加载(final FMLLoadCompleteEvent 事件) {
        Object2IntMap<MobEffect> 等级表 = new Object2IntOpenHashMap<>(Registry.MOB_EFFECT.size());
        for(MobEffect 效果 : Registry.MOB_EFFECT) 等级表.put(效果, 0);
        for(Potion 药水 : Registry.POTION) for(MobEffectInstance 效果 : 药水.getEffects()) 等级表.merge(药水效果集.addOrGet(效果.getEffect()), 效果.getAmplifier(), Math::max);
        for(Object2IntMap.Entry<MobEffect> 等级对 : 等级表.object2IntEntrySet())
            等级配置.put(等级对.getKey(), 构建器.translation(等级对.getKey().getDescriptionId()).define(List.of("效果等级 (Effect Levels)",
                Registry.MOB_EFFECT.getKey(等级对.getKey()).toString()), 等级对.getIntValue()));
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, 构建器.build());
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

    private static List<MobEffect> 效果表(ForgeConfigSpec.ConfigValue<List<? extends String>> 白名单配置) {
        return Lists.transform(白名单配置.get(), Functions.compose(Registry.MOB_EFFECT::get, ResourceLocation::new));
    }

    @SubscribeEvent
    public static void 配置(final ModConfigEvent 事件) {
        效果表 = new ReferenceArrayList<>(Registry.MOB_EFFECT.size());
        if(!自定义配置.get()) {
            Set<MobEffect> 白名单效果集 = Set.copyOf(效果表(白名单配置)), 黑名单效果集 = Set.copyOf(效果表(黑名单配置));
            for(MobEffect 效果 : Registry.MOB_EFFECT)
                if(白名单效果集.contains(效果) || !黑名单效果集.contains(效果) && 药水效果集.contains(效果) && 效果.isBeneficial() && !效果.isInstantenous())
                    效果表.add(new MobEffectInstance(效果, 时长配置.get(), 等级配置.get(效果).get()));
        } else for(MobEffect 效果 : 效果表(效果配置)) if(效果 != null) 效果表.add(new MobEffectInstance(效果, 时长配置.get(), 等级配置.get(效果).get()));
        PotionUtils.setCustomEffects(效果物品组, 效果表 = List.copyOf(效果表));
    }

    @SubscribeEvent
    public static void 注册(final RegisterEvent 事件) {
        if(Registry.ITEM_REGISTRY.equals(事件.getRegistryKey())) {
            事件.register(Registry.ITEM_REGISTRY, new ResourceLocation(MODID, "god_potion_bottle"), Suppliers.ofInstance(神药物品.神药瓶));
            事件.register(Registry.ITEM_REGISTRY, new ResourceLocation(MODID, "water_god_potion_bottle"), Suppliers.ofInstance(神药物品.神药水瓶));
            事件.register(Registry.ITEM_REGISTRY, new ResourceLocation(MODID, "god_potion"), Suppliers.ofInstance(神药物品.神药));
            BrewingRecipeRegistry.addRecipe(Ingredient.of(神药物品.神药水瓶), Ingredient.of(Items.NETHER_STAR), new ItemStack(神药物品.神药));
        }
    }
}