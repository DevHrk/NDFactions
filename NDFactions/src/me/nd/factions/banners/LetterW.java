/*    */ package me.nd.factions.banners;
/*    */ 
/*    */ import org.bukkit.DyeColor;
/*    */ import org.bukkit.inventory.ItemStack;
/*    */ import org.bukkit.inventory.meta.BannerMeta;
/*    */ 
/*    */ public class LetterW extends Letter
/*    */ {
/*    */   public LetterW()
/*    */   {
/* 11 */     super(new char[] { 'W', 'w' });
/*    */   }
/*    */   
/*    */   public ItemStack getBanner(DyeColor foreGroundColor, DyeColor backGroundColor, boolean border) {
/* 15 */     ItemStack banner = getEmptyNamedBanner(getLetters()[0]);
/* 16 */     BannerMeta bannerMeta = (BannerMeta)banner.getItemMeta();
/* 17 */     bannerMeta.setBaseColor(backGroundColor);
/* 18 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.TRIANGLE_BOTTOM));
/* 19 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(backGroundColor, org.bukkit.block.banner.PatternType.TRIANGLES_BOTTOM));
/* 20 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.STRIPE_LEFT));
/* 21 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.STRIPE_RIGHT));
/* 22 */     if (border)
/* 23 */       bannerMeta = addBorders(bannerMeta, backGroundColor);
/* 24 */     banner.setItemMeta(bannerMeta);
/* 25 */     return banner;
/*    */   }
/*    */ }


