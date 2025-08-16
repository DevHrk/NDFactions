package me.nd.factions.banners;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;

import java.util.ArrayList;
import java.util.List;

public class Banners {

	public static ItemStack getAlphabet(ItemStack baseBanner, String name, DyeColor baseColor, DyeColor patternColor) {
		if (name == null || name.isEmpty()) {
			return baseBanner != null ? baseBanner : new ItemStack(Material.WHITE_BANNER);
		}

		String s = name.substring(0, 1).toLowerCase();
		return BannerPatternFactory.createBanner(s, baseBanner, baseColor, patternColor);
	}

	// Factory class to create banner patterns for each letter and number
	private static class BannerPatternFactory {
		public static ItemStack createBanner(String character, ItemStack baseBanner, DyeColor baseColor, DyeColor patternColor) {
			// Determine base banner material based on baseColor
			Material bannerMaterial = getBannerMaterial(baseColor);
			ItemStack banner = (baseBanner != null && baseBanner.getType().name().endsWith("_BANNER"))
					? baseBanner.clone()
					: new ItemStack(bannerMaterial);

			BannerMeta meta = (BannerMeta) banner.getItemMeta();
			if (meta == null) {
				return banner;
			}

			List<Pattern> patterns = getPatternsForCharacter(character, patternColor);
			meta.setPatterns(patterns);
			banner.setItemMeta(meta);
			return banner;
		}

		private static Material getBannerMaterial(DyeColor color) {
			switch (color) {
				case BLACK: return Material.BLACK_BANNER;
				case BLUE: return Material.BLUE_BANNER;
				case BROWN: return Material.BROWN_BANNER;
				case CYAN: return Material.CYAN_BANNER;
				case GRAY: return Material.GRAY_BANNER;
				case GREEN: return Material.GREEN_BANNER;
				case LIGHT_BLUE: return Material.LIGHT_BLUE_BANNER;
				case LIGHT_GRAY: return Material.LIGHT_GRAY_BANNER;
				case LIME: return Material.LIME_BANNER;
				case MAGENTA: return Material.MAGENTA_BANNER;
				case ORANGE: return Material.ORANGE_BANNER;
				case PINK: return Material.PINK_BANNER;
				case PURPLE: return Material.PURPLE_BANNER;
				case RED: return Material.RED_BANNER;
				case YELLOW: return Material.YELLOW_BANNER;
				case WHITE: default: return Material.WHITE_BANNER;
			}
		}

		private static List<Pattern> getPatternsForCharacter(String character, DyeColor patternColor) {
			List<Pattern> patterns = new ArrayList<>();
			switch (character) {
				case "0":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					break;
				case "1":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_CENTER));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "2":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					break;
				case "3":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "4":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					break;
				case "5":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "6":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					break;
				case "7":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					break;
				case "8":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					break;
				case "9":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					break;
				case "a":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					break;
				case "b":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "c":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "d":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					break;
				case "e":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "f":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					break;
				case "g":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					break;
				case "h":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					break;
				case "i":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_CENTER));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "j":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "k":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.DIAGONAL_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.DIAGONAL_RIGHT));
					break;
				case "l":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "m":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.TRIANGLE_TOP));
					break;
				case "n":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.DIAGONAL_LEFT));
					break;
				case "o":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					break;
				case "p":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					break;
				case "q":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.SQUARE_BOTTOM_RIGHT));
					break;
				case "r":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.DIAGONAL_RIGHT));
					break;
				case "s":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_MIDDLE));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "t":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_CENTER));
					break;
				case "u":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "v":
					patterns.add(new Pattern(patternColor, PatternType.TRIANGLE_BOTTOM));
					break;
				case "w":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.TRIANGLE_BOTTOM));
					break;
				case "x":
					patterns.add(new Pattern(patternColor, PatternType.CROSS));
					break;
				case "y":
					patterns.add(new Pattern(patternColor, PatternType.DIAGONAL_LEFT));
					patterns.add(new Pattern(patternColor, PatternType.DIAGONAL_RIGHT));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					break;
				case "z":
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_TOP));
					patterns.add(new Pattern(patternColor, PatternType.STRIPE_BOTTOM));
					patterns.add(new Pattern(patternColor, PatternType.DIAGONAL_RIGHT));
					break;
				default:
					patterns.add(new Pattern(patternColor, PatternType.BORDER));
					break;
			}
			return patterns;
		}
	}
}