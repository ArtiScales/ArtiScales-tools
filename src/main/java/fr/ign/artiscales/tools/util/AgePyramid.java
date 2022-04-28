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
 */

public class AgePyramid {
    public static final String probaFieldName = "proba";
    public static final String ageFieldName = "age";
    private static final IdentityHashMap<Integer, Float> data = new IdentityHashMap<>(106);
    private static InputStream agePyramidFile = AgePyramid.class.getClassLoader().getResourceAsStream("pyramide-des-ages-2017.csv");
    private static boolean init = false;

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
        r.close();
        init = true;
    }

    /**
     * Get a pseudo-random age between included bounds (between 0 and 105 years old)
     *
     * @param boundInf inferior bound
     * @param boundSup superior bound
     * @return one single age
     */
    public static int getRandomAge(int boundInf, int boundSup) {
        return getRandomAge(boundInf, true, boundSup, true);
    }


    /**
     * Get a pseudo-random age between an inferior bounds and people aged 105 years old
     *
     * @param boundInf        inferior bound
     * @param includeBoundInf is the inferior bound included ?
     * @return one single age
     */
    public static int getRandomAge(int boundInf, boolean includeBoundInf) {
        return getRandomAge(boundInf, includeBoundInf, 105, true);
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

        if ((includeBoundSup ? boundSup : boundSup - 1) > 105) {
            System.out.println("getRandomAge() : Superior bound exeeded the maximal age (which is 105). Age put down to that (included)");
            boundSup = 105;
            includeBoundSup = true;
        }
        final float[] probas = fillProba(boundInf, includeBoundInf, boundSup, includeBoundSup);
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

//not faster
//    private static Float[] fillProba(int boundInf, boolean includeBoundInf, int boundSup, boolean includeBoundSup) {
//        return data.keySet().stream().filter(age -> age > (includeBoundInf ? boundInf - 1 : boundInf) && age < (includeBoundSup ? boundSup + 1 : boundSup)).map(data::get).toArray(Float[]::new);
//    }

/*    public static void main(String[] args) {
        long time = System.currentTimeMillis();
        for (int i = 0; i < 10000000; i++)
            getRandomAge(50, 60);
        System.out.println(System.currentTimeMillis() - time);
    }*/
}
