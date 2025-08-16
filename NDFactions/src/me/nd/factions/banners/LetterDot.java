/*    */ package me.nd.factions.banners;
/*    */ 
/*    */ import org.bukkit.DyeColor;
/*    */ import org.bukkit.inventory.ItemStack;
/*    */ import org.bukkit.inventory.meta.BannerMeta;
/*    */ 
/*    */ public class LetterDot extends Letter
/*    */ {
/*    */   public LetterDot()
/*    */   {
/* 11 */     super(new char[] { '.' });
/*    */   }
/*    */   
/*    */   public ItemStack getBanner(DyeColor foreGroundColor, DyeColor backGroundColor, boolean border) {
/* 15 */     ItemStack banner = getEmptyNamedBanner(getLetters()[0]);
/* 16 */     BannerMeta bannerMeta = (BannerMeta)banner.getItemMeta();
/* 17 */     bannerMeta.setBaseColor(backGroundColor);
/* 18 */     bannerMeta.addPattern(new org.bukkit.block.banner.Pattern(foreGroundColor, org.bukkit.block.banner.PatternType.SQUARE_BOTTOM_LEFT));
/* 19 */     if (border)
/* 20 */       bannerMeta = addBorders(bannerMeta, backGroundColor);
/* 21 */     banner.setItemMeta(bannerMeta);
/* 22 */     return banner;
/*    */   }
/*    */ }


