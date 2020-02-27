package fr.ign.cogit.geoToolsFunctions;

import java.io.FileNotFoundException;

import org.opengis.feature.simple.SimpleFeature;

public class Attribute {
	
	/**
	 * Get the indice number on the position of the INSEE number from a shapefile
	 * @param parcel
	 * @return the INSEE number
	 */
	public static int getINSEEIndice(String[] head) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1) {
			if (head[i].toLowerCase().contains("insee") || head[i].toLowerCase().contains("code_insee")
					|| head[i].toLowerCase().contains("depcom")|| head[i].toLowerCase().contains("codecommune")) {
				return i;
			}
		}
		throw new FileNotFoundException(" Attribute.getINSEEindice : no INSEE number found"); 
	}

	/**
	 * Get the indice number on the position of the Latitude field from a .csv
	 * @param head: the header of the .csv file 
	 * @return the indice on which  number
	 */
	public static int getLatIndice (String[] head) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1) {
			if (head[i].toLowerCase().contains("latitude") || head[i].toLowerCase().equals("lat")
					|| head[i].toLowerCase().equals("latitude")) {
				return i;
			}
		}
		throw new FileNotFoundException("Attribute.getLatIndice : no latitude indice found"); 
	}
	
	/**
	 * Get the indice number on the position of the Latitude field from a .csv
	 * @param head: the header of the .csv file 
	 * @return the indice on which  number
	 */
	public static int getLongIndice (String[] head) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1) {
			if (head[i].toLowerCase().contains("longitude") || head[i].toLowerCase().contains("longitude")|| head[i].toLowerCase().equals("long") ) {
				return i;
			}
		}
		throw new FileNotFoundException("Attribute.getLongIndice : no longitude indice found"); 
	}
	
	/**
	 * Get the indice number on the position of the Latitude field from a .csv
	 * @param head: the header of the .csv file 
	 * @return the indice on which  number
	 */
	public static int getZipCodeIndice (String[] head) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1) {
			if (head[i].toLowerCase().contains("codepos") || head[i].toLowerCase().contains("code_post") 
					|| head[i].toLowerCase().contains("zipcode")) {
				return i;
			}
		}
		throw new FileNotFoundException("Attribute.getZipCodeIndice : no zipcode indice found"); 
	}
	
	/**
	 * Get the indice number on the position of the Latitude field from a .csv
	 * @param head: the header of the .csv file 
	 * @return the indice on which  number
	 */
	public static int getIndice (String[] head, String indiceName) throws FileNotFoundException {
		for (int i = 0; i < head.length; i = i + 1) {
			if (head[i].contains(indiceName)) {
				return i;
			}
		}
		throw new FileNotFoundException("Attribute.getIndice : no "+indiceName+" indice found"); 
	}
	
	/**
	 * Construct the french community code number (INSEE) from a french parcel
	 * @param parcel
	 * @return the INSEE number
	 */
	public static String makeINSEECode(SimpleFeature parcel) {
		return ((String) parcel.getAttribute("CODE_DEP")) + ((String) parcel.getAttribute("CODE_COM"));
	}
}
