package admsgenter.god_potion;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class 神药物品 extends Item {
    public static final 神药物品 神药瓶 = new 神药物品(new Properties());
    public static final 神药物品 神药水瓶 = new 神药物品(new Properties());
    public static final 神药物品 神药 = new 神药物品(new Properties());

    public 神药物品(final Properties 属性) { super(属性); }

    @Override
    public void appendHoverText(ItemStack 物品组, Level 世界, List<Component> 提示框, TooltipFlag 标志) { if(this == 神药) PotionUtils.addPotionTooltip(神药模组.效果表, 提示框, 1F); }

    @Override
    public ItemStack finishUsingItem(ItemStack 物品组, Level 世界, LivingEntity 生物) {
        if(this == 神药) if(世界.isClientSide) {
            RandomSource 随机数 = 生物.getRandom();
            int 次数 = 神药模组.粒子配置.get();
            while(--次数 >= 0) 世界.addParticle(ParticleTypes.ENTITY_EFFECT, 生物.getRandomX(.5D), 生物.getRandomY(), 生物.getRandomZ(.5D),
                随机数.nextDouble(), 随机数.nextDouble(), 随机数.nextDouble());
        } else for(MobEffectInstance 效果 : 神药模组.效果表) 生物.addEffect(new MobEffectInstance(效果));
        if(生物 instanceof Player 玩家) {
            if(玩家 instanceof ServerPlayer) CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer) 玩家, 物品组);
            玩家.awardStat(Stats.ITEM_USED.get(this));
            if(!玩家.getAbilities().instabuild) if(物品组.getCount() > 1) {
                物品组.shrink(1);
                ItemStack 瓶 = new ItemStack(神药瓶);
                if(!玩家.addItem(瓶)) 玩家.drop(瓶, true);
            } else 玩家.setItemInHand(玩家.getUsedItemHand(), new ItemStack(神药瓶));
        }
        生物.gameEvent(GameEvent.DRINK);
        return 物品组;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack 物品组) { return UseAnim.DRINK; }

    @Override
    public int getUseDuration(ItemStack 物品组) { return 32; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level 世界, Player 玩家, InteractionHand 主手) {
        if(this == 神药瓶) {
            ItemStack 物品组 = 玩家.getItemInHand(主手);
            BlockHitResult 结果 = getPlayerPOVHitResult(世界, 玩家, ClipContext.Fluid.SOURCE_ONLY);
            if(结果.getType() == HitResult.Type.BLOCK && 世界.mayInteract(玩家, 结果.getBlockPos()) && 世界.getFluidState(结果.getBlockPos()).is(FluidTags.WATER)) {
                世界.playSound(玩家, 玩家.getX(), 玩家.getY(), 玩家.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1F, 1F);
                世界.gameEvent(玩家, GameEvent.FLUID_PICKUP, 结果.getBlockPos());
                玩家.awardStat(Stats.ITEM_USED.get(this));
                return InteractionResultHolder.sidedSuccess(ItemUtils.createFilledResult(物品组, 玩家, new ItemStack(神药水瓶)), 世界.isClientSide());
            }
            return InteractionResultHolder.pass(物品组);
        } else return ItemUtils.startUsingInstantly(世界, 玩家, 主手);
    }
}