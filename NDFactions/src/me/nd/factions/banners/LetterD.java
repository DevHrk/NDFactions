/*    */ package me.nd.factions.banners;
/*    */ 
/*    */ import org.bukkit.DyeColor;
/*    */ import org.bukkit.block.banner.Pattern;
/*    */ import org.bukkit.block.banner.PatternType;
/*    */ import org.bukkit.inventory.ItemStack;
/*    */ import org.bukkit.inventory.meta.BannerMeta;
/*    */ 
/*    */ public class LetterD
/*    */   extends Letter
/*    */ {
/*    */   public LetterD()
/*    */   {
/* 14 */     super(new char[] {'D', 'd' });
/*    */   }
/*    */   
/*    */ 
/*    */   public ItemStack getBanner(DyeColor foreGroundColor, DyeColor backGroundColor, boolean border)
/*    */   {
/* 20 */     ItemStack banner = getEmptyNamedBanner(getLetters()[0]);
/* 21 */     BannerMeta bannerMeta = (BannerMeta)banner.getItemMeta();
/* 22 */     bannerMeta.setBaseColor(backGroundColor);
/* 23 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_LEFT));
/* 24 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_BOTTOM));
/* 25 */     bannerMeta.addPattern(new Pattern(backGroundColor, PatternType.DIAGONAL_RIGHT_MIRROR));
/* 26 */     bannerMeta.addPattern(new Pattern(foreGroundColor, PatternType.STRIPE_DOWNRIGHT));
/* 27 */     if (border)
/* 28 */       bannerMeta = addBorders(bannerMeta, backGroundColor);
/* 29 */     banner.setItemMeta(bannerMeta);
/* 30 */     return banner;
/*    */   }
/*    */ }


