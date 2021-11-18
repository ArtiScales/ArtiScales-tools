package fr.ign.artiscales.tools.io.csv;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Csv {
    public static Character sep = ',', escape = CSVWriter.DEFAULT_ESCAPE_CHARACTER, quote = CSVWriter.DEFAULT_QUOTE_CHARACTER;
    public static String lineEnd = CSVWriter.DEFAULT_LINE_END;

    /**
     * TODO merge the two function together
     *
     * @param f
     * @return
     * @throws IOException
     */
    public static CSVReader getCSVReader(File f) throws IOException {
        CSVReader r = new CSVReaderBuilder(new FileReader(f)).withCSVParser(new CSVParserBuilder().withSeparator(sep).build()).build();
        if (r.readNext().length == 1)
            System.out.println("CSV header has a length of 1. Maybe separator's wrong");
        r.close();
        return new CSVReaderBuilder(new FileReader(f)).withCSVParser(new CSVParserBuilder().withSeparator(sep).build()).build();
    }

    /**
     * TODO do we have to clos anything?
     *
     * @param input
     * @return
     * @throws IOException
     */
    public static CSVReader getCSVReader(InputStream input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        input.transferTo(baos);
        InputStream firstClone = new ByteArrayInputStream(baos.toByteArray());
        InputStream secondClone = new ByteArrayInputStream(baos.toByteArray());
        if (isCSvRightFormat(firstClone))
            return new CSVReaderBuilder(new BufferedReader(new InputStreamReader(secondClone))).withCSVParser(new CSVParserBuilder().withSeparator(sep).build()).build();
        else
            return null;
    }

    private static boolean isCSvRightFormat(InputStream br) throws IOException {
        CSVReader r = new CSVReaderBuilder(new BufferedReader(new InputStreamReader(br))).withCSVParser(new CSVParserBuilder().withSeparator(sep).build()).build();
        if (r.readNext().length == 1) {
            System.out.println("CSV header has a length of 1. Maybe separator's wrong");
            return false;
        }
        r.close();
        return true;
    }

    /**
     * Sometimes dummy data producers are variously using separators for the same data. This method will try to do what's best
     *
     * @param fLine Header of .csv is a clue
     */
    public static char setCharSepAutomaticaly(String fLine) {
        if (fLine.split(",").length > fLine.split(";").length)
            return ',';
        else
            return ';';
    }

    public static CSVWriter getCSVWriter(File f) throws IOException {
        return getCSVWriter(f.getName().endsWith(".csv") ? f : new File(f.getAbsolutePath() + ".csv"), false);
    }

    public static CSVWriter getCSVWriter(File f, boolean append) throws IOException {
        return new CSVWriter(new FileWriter(f, append), sep, quote, escape, lineEnd);
    }
}
