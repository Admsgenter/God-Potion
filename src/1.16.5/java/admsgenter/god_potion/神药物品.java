package admsgenter.god_potion;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class 神药物品 extends Item {
    public static final 神药物品 神药瓶 = new 神药物品(new Properties().tab(ItemGroup.TAB_BREWING));
    public static final 神药物品 神药水瓶 = new 神药物品(new Properties().tab(ItemGroup.TAB_BREWING));
    public static final 神药物品 神药 = new 神药物品(new Properties().tab(ItemGroup.TAB_BREWING));

    public 神药物品(final Properties 属性) { super(属性); }

    @Override
    public void appendHoverText(ItemStack 物品组, World 世界, List<ITextComponent> 提示框, ITooltipFlag 标志) { if(this == 神药) PotionUtils.addPotionTooltip(神药模组.效果物品组, 提示框, 1F); }

    @Override
    public ItemStack finishUsingItem(ItemStack 物品组, World 世界, LivingEntity 生物) {
        if(this == 神药) if(世界.isClientSide) {
            Random 随机数 = 生物.getRandom();
            int 次数 = 神药模组.粒子配置.get();
            while(--次数 >= 0) 世界.addParticle(ParticleTypes.ENTITY_EFFECT, 生物.getRandomX(.5D), 生物.getRandomY(), 生物.getRandomZ(.5D),
                随机数.nextDouble(), 随机数.nextDouble(), 随机数.nextDouble());
        } else for(EffectInstance 效果 : 神药模组.效果表) 生物.addEffect(new EffectInstance(效果));
        if(生物 instanceof PlayerEntity) {
            if(生物 instanceof ServerPlayerEntity) CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayerEntity) 生物, 物品组);
            ((PlayerEntity) 生物).awardStat(Stats.ITEM_USED.get(this));
            if(!((PlayerEntity) 生物).abilities.instabuild) if(物品组.getCount() > 1) {
                物品组.shrink(1);
                ItemStack 瓶 = new ItemStack(神药瓶);
                if(!((PlayerEntity) 生物).addItem(瓶)) ((PlayerEntity) 生物).drop(瓶, true);
            } else 生物.setItemInHand(生物.getUsedItemHand(), new ItemStack(神药瓶));
        }
        return 物品组;
    }

    @Override
    public UseAction getUseAnimation(ItemStack 物品组) { return UseAction.DRINK; }

    @Override
    public int getUseDuration(ItemStack 物品组) { return 32; }

    @Override
    public ActionResult<ItemStack> use(World 世界, PlayerEntity 玩家, Hand 主手) {
        if(this == 神药瓶) {
            ItemStack 物品组 = 玩家.getItemInHand(主手);
            BlockRayTraceResult 结果 = getPlayerPOVHitResult(世界, 玩家, RayTraceContext.FluidMode.SOURCE_ONLY);
            if(结果.getType() == RayTraceResult.Type.BLOCK && 世界.mayInteract(玩家, 结果.getBlockPos()) && 世界.getFluidState(结果.getBlockPos()).is(FluidTags.WATER)) {
                世界.playSound(玩家, 玩家.getX(), 玩家.getY(), 玩家.getZ(), SoundEvents.BOTTLE_FILL, SoundCategory.NEUTRAL, 1F, 1F);
                玩家.awardStat(Stats.ITEM_USED.get(this));
                return ActionResult.sidedSuccess(DrinkHelper.createFilledResult(物品组, 玩家, new ItemStack(神药水瓶)), 世界.isClientSide());
            }
            return ActionResult.pass(物品组);
        } else return DrinkHelper.useDrink(世界, 玩家, 主手);
    }
}