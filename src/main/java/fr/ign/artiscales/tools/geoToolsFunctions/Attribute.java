package fr.ign.artiscales.tools.geoToolsFunctions;

import java.io.FileNotFoundException;
import java.rmi.server.UID;

public class Attribute {

	public static String makeUniqueId() {
		return String.valueOf(Math.random() * 100000) + new UID().toString().replace(':', '_');
	}

	/**
	 * Get the indice number on the position of the Latitude field from a .csv
	 * 
	 * @param head
	 *            the header of the .csv file
	 * @return the indice on which number
	 */
	public static int getLatIndice(String[] head) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1) {
			if (head[i].toLowerCase().contains("latitude") || head[i].toLowerCase().equals("lat") || head[i].toLowerCase().equals("latitude")
					|| head[i].toLowerCase().equals("x") || head[i].toLowerCase().equals("lambert_x")
					|| (head[i].toLowerCase().contains("x") && head[i].toLowerCase().contains("coord")))
				return i;
		}
		throw new FileNotFoundException("Attribute.getLatIndice : no latitude indice found");
	}

	/**
	 * Get the indice number on the position of the Latitude field from a .csv
	 * 
	 * @param head
	 *            the header of the .csv file
	 * @return the indice on which number
	 */
	public static int getLongIndice(String[] head) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1) {
			if (head[i].toLowerCase().contains("longitude") || head[i].toLowerCase().contains("longitude") || head[i].toLowerCase().equals("long")
					|| head[i].toLowerCase().equals("y") || head[i].toLowerCase().equals("lambert_y")
					|| (head[i].toLowerCase().contains("y") && head[i].toLowerCase().contains("coord")))
				return i;
		}
		throw new FileNotFoundException("Attribute.getLongIndice : no longitude indice found");
	}

	/**
	 * Get the indice number on the position of the Latitude field from a .csv
	 * 
	 * @param head
	 *            the header of the .csv file
	 * @return the indice on which number
	 */
	public static int getCityCodeIndice(String[] head) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1) {
			String word = head[i].toLowerCase();
			if (word.contains("codepos") || word.contains("code_post") || word.contains("zipcode") || head[i].toLowerCase().contains("insee")
					|| head[i].toLowerCase().contains("code_insee") || head[i].toLowerCase().contains("depcom")
					|| head[i].toLowerCase().contains("codecommune"))
				return i;
		}
		throw new FileNotFoundException("Attribute.getZipCodeIndice : no zipcode indice found");
	}

	/**
	 * Get the indice number on the position of the Latitude field from a .csv
	 * 
	 * @param head
	 *            the header of the .csv file
	 * @return the indice on which number
	 */
	public static int getIndice(String[] head, String indiceName) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1)
			if (head[i].contains(indiceName))
				return i;
		throw new FileNotFoundException("Attribute.getIndice : no " + indiceName + " indice found");
	}
}
