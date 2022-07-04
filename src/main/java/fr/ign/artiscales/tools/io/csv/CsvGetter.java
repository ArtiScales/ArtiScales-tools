package fr.ign.artiscales.tools.io.csv;

import com.opencsv.CSVReader;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CsvGetter extends Csv {

    /**
     * Get a list of unique values for a given field from a .csv file
     *
     * @param csvFile   csv file
     * @param fieldName name of the field to sum values
     * @return a list of unique values
     */
    public static List<String> getUniqueFieldValue(File csvFile, String fieldName) throws IOException {
        CSVReader r = getCSVReader(csvFile);
        int i = Attribute.getIndice(r.readNext(), fieldName);
        List<String> result = new ArrayList<>();
        for (String[] l : r.readAll())
            if (!result.contains(l[i]))
                result.add(l[i]);
        r.close();
        return result;
    }

    /**
     * Get the number of occurrences for each unique values for a given field from a .csv file
     *
     * @param csvFile   csv file
     * @param fieldName name of the field to sum values
     * @return a list of unique values
     */
    public static HashMap<String, Integer> countFieldValue(File csvFile, String fieldName) throws IOException {
        CSVReader r = getCSVReader(csvFile);
        int i = Attribute.getIndice(r.readNext(), fieldName);
        HashMap<String, Integer> result = new HashMap<>();
        for (String[] l : r.readAll())
            if (!result.containsKey(l[i]))
                result.put(l[i], 1);
            else
                result.put(l[i], result.get(l[i]) + 1);
        r.close();
        return result;
    }


    /**
     * Get the values of cells corresponding to the value of another field's cell.
     *
     * @param csvFile              CSV {@link File} with a comma as a separator
     * @param targetAttributeName  name of the field that will be compared
     * @param targetAttributeValue value of the cell that will be compared
     * @param wantedAttributeName  name of the field of the wanted cell
     * @return the value of the cells in an {@link ArrayList}
     * @throws IOException by CSVReader
     */
    public static List<String> getCells(File csvFile, String targetAttributeName, String targetAttributeValue, String wantedAttributeName) throws IOException {
        return getCells(csvFile, new String[]{targetAttributeName}, new String[]{targetAttributeValue}, wantedAttributeName);
    }

    /**
     * Return true if the combination of field's values are found in a CSV reader.
     *
     * @param r                     CSV reader with a header
     * @param targetAttributeNames  names of the field that will be compared.
     * @param targetAttributeValues values of the field that will be compared. Must be in the same order (and the same length) than the #targetAttributeNames.
     * @return the values of the cell
     * @throws IOException by CSVReader
     */
    public static boolean isCellsContainCorrespondingCombination(CSVReader r, String[] targetAttributeNames, String[] targetAttributeValues) throws IOException {
        String[] fLine = r.readNext();
        for (String[] line : r.readAll()) {
            boolean add = true;
            for (int i = 0; i < targetAttributeNames.length; i++)
                if (!line[Attribute.getIndice(fLine, targetAttributeNames[i])].equals(targetAttributeValues[i])) {
                    add = false;
                    break;
                }
            if (add)
                return true;
        }
        return false;
    }

    /**
     * Get the value of a cell corresponding to the value of multiple field's cells.
     *
     * @param csvFile               CSV {@link File}
     * @param targetAttributeNames  name of the field that will be compared
     * @param targetAttributeValues values of the cells that will be compared
     * @param wantedAttributeName   name of the field of the wanted cell
     * @return the values of the cell
     * @throws IOException by CSVReader
     */
    public static List<String> getCells(File csvFile, String[] targetAttributeNames, String[] targetAttributeValues, String wantedAttributeName) throws IOException {
        CSVReader r = getCSVReader(csvFile);
        String[] fLine = r.readNext();
        List<String> result = new ArrayList<>();
        for (String[] line : r.readAll()) {
            boolean add = true;
            for (int i = 0; i < targetAttributeNames.length; i++)
                if (!line[Attribute.getIndice(fLine, targetAttributeNames[i])].equals(targetAttributeValues[i])) {
                    add = false;
                    break;
                }
            if (add)
                result.add(line[Attribute.getIndice(fLine, wantedAttributeName)]);
        }
        r.close();
        return result;
    }

    /**
     * Get the value of a cell corresponding to the value of another field's cell. Unique result, stop at first.
     *
     * @param csvFile              CSV {@link File} with a comma as a separator
     * @param targetAttributeName  name of the field that will be compared
     * @param targetAttributeValue value of the cell that will be compared
     * @param wantedAttributeName  name of the field of the wanted cell
     * @return the values of the cell
     * @throws IOException by CSVReader
     */
    public static String getCell(File csvFile, String targetAttributeName, String targetAttributeValue, String wantedAttributeName) throws IOException {
        CSVReader r = getCSVReader(csvFile);
        String[] fLine = r.readNext();
        int iTarget = Attribute.getIndice(fLine, targetAttributeName);
        String result = "";
        for (String[] line : r.readAll())
            if (line[iTarget].equals(targetAttributeValue)) {
                result = line[Attribute.getIndice(fLine, wantedAttributeName)];
                break;
            }
        r.close();
        return result;
    }

    /**
     * Get the value of a cell corresponding to the value of another field's cell. Unique result, stop at first.
     *
     * @param csvFile               CSV {@link File} with a comma as a separator
     * @param targetAttributeNames  names of the fields that will be compared
     * @param targetAttributeValues values of the cells that will be compared
     * @return the values of the cell
     * @throws IOException by CSVReader
     */
    public static String[] getLine(File csvFile, String[] targetAttributeNames, String[] targetAttributeValues) throws IOException {
        CSVReader r = getCSVReader(csvFile);
        String[] fLine = r.readNext();
        String[] result = new String[fLine.length];
        for (String[] line : r.readAll()) {
            boolean add = true;
            for (int i = 0; i < targetAttributeNames.length; i++)
                if (!line[Attribute.getIndice(fLine, targetAttributeNames[i])].equals(targetAttributeValues[i])) {
                    add = false;
                    break;
                }
            if (add) {
                result = line;
                break;
            }
        }
        r.close();
        return result;
    }

    public static String getCell(InputStream csvInputStream, String targetAttributeName, String targetAttributeValue, String wantedAttributeName) throws IOException {
        CSVReader r = getCSVReader(csvInputStream);
        assert r != null;
        String[] fLine = r.readNext();
        int iTarget = Attribute.getIndice(fLine, targetAttributeName);
        String[] line;
        String result = "";
        while ((line = r.readNext()) != null) {
            if (line[iTarget].equals(targetAttributeValue)) {
                result = line[Attribute.getIndice(fLine, wantedAttributeName)];
                break;
            }
        }
        r.close();
        return result;
    }

    /**
     * Get the indice number on the position of the header of a .csv file
     *
     * @param csvFile   .csv file with a header
     * @param fieldName name of the field to get indice from
     * @return the indice on which number
     */
    public static int getIndice(File csvFile, String fieldName) throws IOException {
        CSVReader r = getCSVReader(csvFile);
        int i = Attribute.getIndice(r.readNext(), fieldName);
        r.close();
        return i;
    }


}
