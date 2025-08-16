/*    */ package me.nd.factions.banners;
/*    */ 
/*    */ import org.bukkit.DyeColor;
/*    */ import org.bukkit.inventory.ItemStack;
/*    */ import org.bukkit.inventory.meta.BannerMeta;
/*    */ 
/*    */ public class LetterI extends Letter
/*    */ {
/*    */   public LetterI()
/*    */   {
/* 11 */     super(new char[] { 'I', 'i' });
/*    */   }
/*    */   
/*    */   public ItemStack getBanner(DyeColor foreGroundColor, DyeColor backGroundColor, boolean border) {
/* 15 */     ItemStack banner = getEmptyNamedBanner(getLetters()[0]);
/* 16 */     BannerMeta bannerMeta = (BannerMeta)banner.getItemMeta();
/* 17 */     bannerMeta.setBaseColor(backGroundColor);
/* 18 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.STRIPE_TOP));
/* 19 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.STRIPE_BOTTOM));
/* 20 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.STRIPE_CENTER));
/* 21 */     if (border)
/* 22 */       bannerMeta = addBorders(bannerMeta, backGroundColor);
/* 23 */     banner.setItemMeta(bannerMeta);
/* 24 */     return banner;
/*    */   }
/*    */ }


