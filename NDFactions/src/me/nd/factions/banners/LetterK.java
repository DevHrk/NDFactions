/*    */ package me.nd.factions.banners;
/*    */ 
/*    */ import org.bukkit.block.banner.Pattern;
/*    */ import org.bukkit.block.banner.PatternType;
/*    */ import org.bukkit.inventory.meta.BannerMeta;
/*    */ 
/*    */ public class LetterK extends Letter
/*    */ {
/*    */   public LetterK()
/*    */   {
/* 11 */     super(new char[] { 'K', 'k' });
/*    */   }
/*    */   
/*    */   public org.bukkit.inventory.ItemStack getBanner(org.bukkit.DyeColor foreGroundColor, org.bukkit.DyeColor backGroundColor, boolean border) {
/* 15 */     org.bukkit.inventory.ItemStack banner = getEmptyNamedBanner(getLetters()[0]);
/* 16 */     BannerMeta bannerMeta = (BannerMeta)banner.getItemMeta();
/* 17 */     bannerMeta.setBaseColor(backGroundColor);
/* 18 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_LEFT));
/* 19 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_LEFT));
/* 20 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_MIDDLE));
/* 21 */     bannerMeta.addPattern(new Pattern(backGroundColor, PatternType.HALF_VERTICAL_MIRROR));
/* 22 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.CROSS));
/* 23 */     if (border)
/* 24 */       bannerMeta = addBorders(bannerMeta, backGroundColor);
/* 25 */     banner.setItemMeta(bannerMeta);
/* 26 */     return banner;
/*    */   }
/*    */ }

