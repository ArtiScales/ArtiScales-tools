package fr.ign.artiscales.tools.util;

import fr.ign.artiscales.tools.io.csv.CsvGetter;

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
            return CsvGetter.getCell(Objects.requireNonNull(FrenchAdmin.class.getClassLoader().getResourceAsStream("zipCommInsee21.csv")), "Code_postal", zip, "Code_commune_INSEE");
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
            return CsvGetter.getCell(Objects.requireNonNull(FrenchAdmin.class.getClassLoader().getResourceAsStream("zipCommInsee21.csv")), "Code_commune_INSEE", insee, "Code_postal");
        } catch (IOException e) {
            System.out.println("INSEE number not found in base");
            e.printStackTrace();
            return "";
        }
    }

    public static String getNameRegionFromCode(String codeRegion) {
         switch (codeRegion) {
            case "01" : return  "Guadeloupe";
            case "02" : return  "Martinique";
            case "03" : return  "Guyane";
            case "04" : return  "La Réunion";
            case "06" : return  "Mayotte";
            case "11" : return  "Île-de-France";
            case "24" : return  "Centre-Val de Loire";
            case "27" : return  "Bourgogne-Franche-Comté";
            case "28" : return  "Normandie";
            case "32" : return  "Nord-Pas-de-Calais-Picardie";
            case "44" : return  "Alsace-Champagne-Ardenne-Lorraine";
            case "52" : return  "Pays de la Loire";
            case "53" : return  "Bretagne";
            case "75" : return  "Aquitaine-Limousin-Poitou-Charentes";
            case "76" : return  "Languedoc-Roussillon-Midi-Pyrénées";
            case "84" : return  "Auvergne-Rhône-Alpes";
            case "93" : return  "Provence-Alpes-Côte d'Azur";
            case "94" : return  "Corse";
        };
        throw new IllegalArgumentException("getNameRegionFromCode() : unknown code ("+codeRegion+")");

    }
}
