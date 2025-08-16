/*    */ package me.nd.factions.banners;

/*    */ import java.util.concurrent.atomic.AtomicInteger;

/*    */ import org.bukkit.DyeColor;
/*    */ import org.bukkit.Material;
/*    */ import org.bukkit.block.banner.Pattern;
/*    */ import org.bukkit.block.banner.PatternType;
/*    */ import org.bukkit.inventory.ItemStack;
/*    */ import org.bukkit.inventory.meta.BannerMeta;

/*    */
/*    */
/*    */
/*    */ public abstract class Letter
/*    */ {
	/* 18 */ public static AtomicInteger getCounter() {
		return counter;
	}

	private static AtomicInteger counter = new AtomicInteger();

	/*    */
	/* 20 */ public char[] getLetters() {
		return this.letters;
	}

	/*    */
	/*    */ public Letter(char[] letters) {
		/* 23 */ if (letters[0] == 0) {
			/* 24 */ throw new IllegalArgumentException("letters[0] is 0 in " + getClass().getSimpleName() + "!");
			/*    */ }
		/* 26 */ this.letters = letters;
		/*    */ }

	/*    */
	/*    */
	/*    */ private char[] letters;

	/*    */ protected ItemStack getEmptyBanner()
	/*    */ {
		/* 34 */ ItemStack banner = new ItemStack(Material.BANNER);
		/* 35 */ return banner;
		/*    */ }

	/*    */
	/*    */ protected ItemStack getEmptyNamedBanner(char letter) {
		/* 39 */ ItemStack banner = getEmptyBanner();
		/*    */
		/* 44 */ return banner;
		/*    */ }

	/*    */
	/*    */ protected BannerMeta addBorders(BannerMeta bannerMeta, DyeColor backGroundColor) {
		/* 48 */ bannerMeta.addPattern(new Pattern(backGroundColor, PatternType.BORDER));
		/*    */
		/* 50 */ return bannerMeta;
		/*    */ }

	/*    */
	/*    */
	/*    */ public abstract ItemStack getBanner(DyeColor paramDyeColor1, DyeColor paramDyeColor2,
			boolean paramBoolean);
	/*    */ }
