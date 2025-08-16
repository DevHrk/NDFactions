package me.nd.factions.banners;

import org.bukkit.DyeColor;
import org.bukkit.inventory.ItemStack;

public class Banners
{

	public static ItemStack getAlphabet(ItemStack a,String name, DyeColor b, DyeColor c){
		String s = name.substring(0, 1).toLowerCase();
		switch (s){
		case "0":
			return new Letter0().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "1":
			return new Letter1().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "2":
			return new Letter2().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "3":
			return new Letter3().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "4":
			return new Letter4().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "5":
			return new Letter5().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "6":
			return new Letter6().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "7":
			return new Letter7().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "8":
			return new Letter8().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "9":
			return new Letter9().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "a":
			return new LetterA().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "b":
			return new LetterB().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "c":
			return new LetterC().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "d":
			return new LetterD().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "e":
			return new LetterE().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "f":
			return new LetterF().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "g":
			return new LetterG().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "h":
			return new LetterH().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "i":
			return new LetterI().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "j":
			return new LetterJ().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "k":
			return new LetterK().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "l":
			return new LetterL().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "m":
			return new LetterM().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "n":
			return new LetterN().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "o":
			return new LetterO().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "p":
			return new LetterP().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "q":
			return new LetterQ().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "r":
			return new LetterR().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "s":
			return new LetterS().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "t":
			return new LetterT().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "u":
			return new LetterU().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "v":
			return new LetterV().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "w":
			return new LetterW().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "x":
			return new LetterX().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "y":
			return new LetterY().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		case "z":
			return new LetterZ().getBanner(DyeColor.BLACK, DyeColor.WHITE, true);
		}
		return null;
	}
	
}
