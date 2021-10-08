package fr.ign.artiscales.tools.util;

import fr.ign.artiscales.tools.io.csv.CsvOp;

import java.io.IOException;
import java.util.Objects;

/**
 * Usual classes for dealing with french administration
 */
public class FrenchAdmin {
 /*   public static void main(String[] args) {
        long t, t1 = 0, t2 = 0;

        for (int i = 0; i < 50; i++) {
            t = System.currentTimeMillis();
            convertZipToInsee("42400");
            t1 += System.currentTimeMillis() - t;


            t = System.currentTimeMillis();
            convertZipToInsee("42400");
            t2 += System.currentTimeMillis() - t;
        }
        System.out.println("Benchmarking\n\tMethod 1 took + " + t1 + " ms\n\tMethod 2 took " + t2 + " ms");
    }*/

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
}
