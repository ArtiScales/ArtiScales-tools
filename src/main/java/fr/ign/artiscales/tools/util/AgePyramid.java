package fr.ign.artiscales.tools.util;

import com.opencsv.CSVReader;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.io.csv.Csv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.Random;

/**
 * Model the probability of age regarding an input age pyramid.
 * Metropolitan France is the default file, but it easily can be changed with the {@link #setAgePyramidFile(File)} method.
 * Regional pyramid for multiple dates can be found here : https://www.insee.fr/fr/outil-interactif/5014911/pyramide.htm#!y=2017&v=2&t=2&c=11
 */

public class AgePyramid {
    public static final String probaFieldName = "proba";
    public static final String ageFieldName = "age";
    private static final IdentityHashMap<Integer, Float> data = new IdentityHashMap<>(106);
    private static InputStream agePyramidFile = AgePyramid.class.getClassLoader().getResourceAsStream("pyramide_des_ages_2017_FrMetro.csv");
    private static boolean init = false;
    private static int maxAge;

    /**
     * Select a predefined French region for its age pyramid.
     *
     * @param region region number (see {@link FrenchAdmin#getNameRegionFromCode(String)}
     */
    public static void setRegionalPyramid(String region) {
        agePyramidFile = AgePyramid.class.getClassLoader().getResourceAsStream("pyramide_des_ages_2017_reg-" + region + ".csv");
        init = false;
    }

    /**
     * Set new .csv file. Must have a ',' separator and fields <i>age</i> and <i>proba</i>.
     *
     * @param agePyramidFile new input file to read the age probabilities from.
     */
    public static void setAgePyramidFile(File agePyramidFile) throws FileNotFoundException {
        AgePyramid.agePyramidFile = new FileInputStream(agePyramidFile);
        init = false;
    }

    /**
     * This method runs if the terms haven't been initialized : data is transfered from the .csv age pyramid description to in-memory.
     * Avoid redundant reading when a large number of ages must be generated.
     * Is reinitialized if a new pyramid file is set.
     *
     * @throws IOException reading the .csv file
     */
    private static void init() throws IOException {
        final CSVReader r = Csv.getCSVReader(agePyramidFile);
        assert r != null;
        final String[] fLine = r.readNext();
        for (String[] l : r.readAll())
            data.put(Integer.parseInt(l[Attribute.getIndice(fLine, ageFieldName)]), Float.parseFloat(l[Attribute.getIndice(fLine, probaFieldName)]));
        maxAge = data.size() - 1;
        r.close();
        init = true;
    }

    /**
     * Get a pseudo-random age between included bounds (between 0 and the maximal age)
     *
     * @param boundInf inferior bound
     * @param boundSup superior bound
     * @return one single age
     */
    public static int getRandomAge(int boundInf, int boundSup) {
        return getRandomAge(boundInf, true, boundSup, true);
    }


    /**
     * Get a pseudo-random age between an inferior bounds and people aged of the maximal age
     *
     * @param boundInf        inferior bound
     * @param includeBoundInf is the inferior bound included ?
     * @return one single age
     */
    public static int getRandomAge(int boundInf, boolean includeBoundInf) {
        if (!init)
            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return getRandomAge(boundInf, includeBoundInf, maxAge, true);
    }

    /**
     * Get a pseudo-random age between an inferior bounds and a superior bound
     *
     * @param boundInf        inferior bound
     * @param includeBoundInf is the inferior bound included ?
     * @return one single age
     */
    public static int getRandomAge(int boundInf, boolean includeBoundInf, int boundSup, boolean includeBoundSup) {
        if (!init)
            try {
                init();
            } catch (IOException e) {
                e.printStackTrace();
            }
        if (boundInf >= boundSup)
            throw new IllegalArgumentException("getRandomAge() : Inf bound is inferior or equal to sup bound");

        if ((includeBoundSup ? boundSup : boundSup - 1) > maxAge) {
            System.out.println("getRandomAge() : Superior bound exceeded the maximal age (which is " + maxAge + "). Age put down to that (included)");
            boundSup = maxAge;
            includeBoundSup = true;
        }
        final float[] probas = fillProba(boundInf, includeBoundInf, boundSup, includeBoundSup);
//        final Float[] probas = fillProba(boundInf, includeBoundInf, boundSup, includeBoundSup);
        final float rnd = new Random().nextFloat();
        float sumProba = 0f;
        for (float f : probas)
            sumProba += f;
        float sum = 0f;
        for (int range = 0; range < probas.length; range++) {
            sum += probas[range] * (1 / sumProba); //normalize proba to 1
            if (rnd <= sum)
                return (includeBoundInf ? boundInf : boundInf + 1) + range;
        }
        return includeBoundSup ? boundSup : boundSup - 1; //sometimes, rounding isnot hyper equal to 1. We then return the last bound
    }

    private static float[] fillProba(int boundInf, boolean includeBoundInf, int boundSup, boolean includeBoundSup) {
        final float[] probas = new float[(includeBoundSup ? boundSup : boundSup - 1) - (includeBoundInf ? boundInf - 1 : boundInf)];
        int i = 0;
        for (int age : data.keySet())
            if (age > (includeBoundInf ? boundInf - 1 : boundInf) && age < (includeBoundSup ? boundSup + 1 : boundSup))
                probas[i++] = data.get(age);
        return probas;
    }

////not faster
//    private static Float[] fillProba(int boundInf, boolean includeBoundInf, int boundSup, boolean includeBoundSup) {
//        return data.keySet().stream().filter(age -> age > (includeBoundInf ? boundInf - 1 : boundInf) && age < (includeBoundSup ? boundSup + 1 : boundSup)).map(data::get).toArray(Float[]::new);
//    }

//    public static void main(String[] args) throws FileNotFoundException {
//        long time = System.currentTimeMillis();
//        for (int i = 0; i < 1000000; i++)
//            System.out.println(getRandomAge(95, 99));
//        System.out.println(System.currentTimeMillis() - time);
//    }
}
