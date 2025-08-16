/*    */ package me.nd.factions.banners;
/*    */ 
/*    */ import org.bukkit.DyeColor;
/*    */ import org.bukkit.inventory.ItemStack;
/*    */ import org.bukkit.inventory.meta.BannerMeta;
/*    */ 
/*    */ public class LetterB extends Letter
/*    */ {
/*    */   public LetterB()
/*    */   {
/* 11 */     super(new char[] { 'B', 'b' });
/*    */   }
/*    */   
/*    */   public ItemStack getBanner(DyeColor foreGroundColor, DyeColor backGroundColor, boolean border) {
/* 15 */     ItemStack banner = getEmptyNamedBanner(getLetters()[0]);
/* 16 */     BannerMeta bannerMeta = (BannerMeta)banner.getItemMeta();
/* 17 */     bannerMeta.setBaseColor(foreGroundColor);
/* 18 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(backGroundColor, org.bukkit.block.banner.PatternType.STRIPE_CENTER));
/* 19 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.STRIPE_BOTTOM));
/* 20 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.STRIPE_TOP));
/* 21 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.STRIPE_MIDDLE));
/* 22 */     if (border)
/* 23 */       bannerMeta = addBorders(bannerMeta, backGroundColor);
/* 24 */     banner.setItemMeta(bannerMeta);
/* 25 */     return banner;
/*    */   }
/*    */ }


