package fr.ign.artiscales.tools.util;

import fr.ign.artiscales.tools.io.csv.CsvOp;

import java.io.IOException;
import java.util.Objects;

/**
 * Usual classes for dealing with french administration
 */
public class FrenchAdmin {
    /**
     * Convert a zip code to an insee code.
     *
     * @param zip zip code
     * @return the corresponding insee code
     */
    public static String convertZipToInsee(String zip) {
        try {
            return CsvOp.getCell(Objects.requireNonNull(FrenchAdmin.class.getClassLoader().getResourceAsStream("zipCommInsee21.csv")), "Code_postal", zip, "Code_commune_INSEE");
        } catch (IOException e) {
            System.out.println("Zip not found in base");
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Convert an insee code to an zip code.
     *
     * @param insee insee code
     * @return the corresponding zip code
     */
    public static String convertInseeToZip(String insee) {
        try {
            return CsvOp.getCell(Objects.requireNonNull(FrenchAdmin.class.getClassLoader().getResourceAsStream("zipCommInsee21.csv")), "Code_commune_INSEE", insee, "Code_postal");
        } catch (IOException e) {
            System.out.println("INSEE number not found in base");
            e.printStackTrace();
            return "";
        }
    }

    public static String getNameRegionFromCode(String codeRegion) {
        return switch (codeRegion) {
            case "01" -> "Guadeloupe";
            case "02" -> "Martinique";
            case "03" -> "Guyane";
            case "04" -> "La Réunion";
            case "06" -> "Mayotte";
            case "11" -> "Île-de-France";
            case "24" -> "Centre-Val de Loire";
            case "27" -> "Bourgogne-Franche-Comté";
            case "28" -> "Normandie";
            case "32" -> "Nord-Pas-de-Calais-Picardie";
            case "44" -> "Alsace-Champagne-Ardenne-Lorraine";
            case "52" -> "Pays de la Loire";
            case "53" -> "Bretagne";
            case "75" -> "Aquitaine-Limousin-Poitou-Charentes";
            case "76" -> "Languedoc-Roussillon-Midi-Pyrénées";
            case "84" -> "Auvergne-Rhône-Alpes";
            case "93" -> "Provence-Alpes-Côte d'Azur";
            case "94" -> "Corse";
            default -> throw new IllegalArgumentException("getNameRegionFromCode() : unknown code ("+codeRegion+")");
        };
    }
}
