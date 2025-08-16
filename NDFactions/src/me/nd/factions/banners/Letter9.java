/*    */ package me.nd.factions.banners;
/*    */ 
/*    */ import org.bukkit.block.banner.Pattern;
/*    */ import org.bukkit.block.banner.PatternType;
/*    */ import org.bukkit.inventory.meta.BannerMeta;
/*    */ 
/*    */ public class Letter9 extends Letter
/*    */ {
/*    */   public Letter9()
/*    */   {
/* 11 */     super(new char[] { '9' });
/*    */   }
/*    */   
/*    */   public org.bukkit.inventory.ItemStack getBanner(org.bukkit.DyeColor foreGroundColor, org.bukkit.DyeColor backGroundColor, boolean border) {
/* 15 */     org.bukkit.inventory.ItemStack banner = getEmptyNamedBanner(getLetters()[0]);
/* 16 */     BannerMeta bannerMeta = (BannerMeta)banner.getItemMeta();
/* 17 */     bannerMeta.setBaseColor(foreGroundColor);
/* 18 */     bannerMeta.addPattern(new Pattern(backGroundColor, PatternType.HALF_HORIZONTAL));
/* 19 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_LEFT));
/* 20 */     bannerMeta.addPattern(new Pattern(backGroundColor, PatternType.STRIPE_BOTTOM));
/* 21 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_TOP));
/* 22 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_RIGHT));
/* 23 */     if (border)
/* 24 */       bannerMeta = addBorders(bannerMeta, backGroundColor);
/* 25 */     banner.setItemMeta(bannerMeta);
/* 26 */     return banner;
/*    */   }
/*    */ }


