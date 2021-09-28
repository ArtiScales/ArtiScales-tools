package fr.ign.artiscales.tools.io.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CsvOp extends Csv {

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
     * Get the value of a cell corresponding to the value of multiple field's cells.
     *
     * @param csvFile               CSV {@link File} with a comma as a separator
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


    /**
     * Add some basic statistic for a predetermined column of a csv file. Add extra lines to the end of the .csv.
     *
     * @param csvFile    path to the .csv {@link File}
     * @param nbCol      number of the column to calulate stats (begins at 0)
     * @param isFirstCol true if there's a header and it will be ignored
     * @throws IOException reading .csv file
     */
    public static void calculateColumnsBasicStat(File csvFile, int nbCol, boolean isFirstCol) throws IOException {
        CSVReader r = getCSVReader(csvFile);
        String[] fCol = null;
        if (isFirstCol)
            fCol = r.readNext();
        List<String[]> lines = r.readAll();
        DescriptiveStatistics ds = new DescriptiveStatistics();
        for (String[] line : lines) {
            try {
                ds.addValue(Double.parseDouble(line[nbCol]));
            } catch (NumberFormatException ignored) {
            }
        }
        r.close();
        CSVWriter w = getCSVWriter(csvFile);
        if (isFirstCol)
            w.writeNext(fCol);
        w.writeAll(lines);

        String[] line = new String[lines.get(0).length];
        line[0] = "mean";
        line[nbCol] = String.valueOf(ds.getMean());
        w.writeNext(line);

        line[0] = "median";
        line[nbCol] = String.valueOf(ds.getPercentile(50));
        w.writeNext(line);

        line[0] = "min";
        line[nbCol] = String.valueOf(ds.getMin());
        w.writeNext(line);

        line[0] = "max";
        line[nbCol] = String.valueOf(ds.getMax());
        w.writeNext(line);

        line[0] = "StandardDeviation";
        line[nbCol] = String.valueOf(ds.getStandardDeviation());
        w.writeNext(line);

        line[0] = "sum";
        line[nbCol] = String.valueOf(ds.getSum());
        w.writeNext(line);
        w.close();
    }

    /**
     * Create a String out of a CSV line with only the indicated indexes. Separate the values with a "-"
     *
     * @param headers list of indexes to put
     * @param line    the csv line in a tab
     * @return the concatenated string
     */
    public static String makeLine(List<Integer> headers, String[] line) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < line.length; i++)
            if (headers.contains(i))
                result.append("-").append(line[i]);
        return result.substring(1, result.length());
    }

    /**
     * Join to CSV by searching a common value in predefined fields. The whole line of the <i>base</i> csv is copied and only the precised fields of the <i>toJoin</i> .csv are copied.
     *
     * @param csvBase               File that will be copied in a whole
     * @param csvToJoin             File to join
     * @param fieldCsvBase          Filed of the base file to make the join
     * @param fieldCsvToJoin        Filed of the ToJoin file to make the join
     * @param addNoCorrespondance   If correspondance between base and toJoin not found, do we still copy the base line ?
     * @param listFieldsToJoin      List of fields from the ToJoin file to copy
     * @param doNotAddDoubledFields does not copy fields if they are already present in the base fields (in case of doubled attributes)
     * @param fileOut               File where to write the output
     * @return the wrote new CSV file
     * @throws IOException Reading and writing files
     * @deprecated bugged
     */
    public static File joinCSVs(File csvBase, File csvToJoin, String fieldCsvBase, String fieldCsvToJoin, boolean addNoCorrespondance, List<String> listFieldsToJoin, boolean doNotAddDoubledFields, File fileOut) throws IOException {
        return joinCSVs(csvBase, csvToJoin, new String[]{fieldCsvBase}, new String[]{fieldCsvToJoin}, addNoCorrespondance, listFieldsToJoin, doNotAddDoubledFields, fileOut);
    }

    /**
     * Join to CSV by searching a common value in predefined fields. The whole line of the <i>base</i> csv and its corresponding line are copied.
     *
     * @param csvToJoin             File to join
     * @param fieldCsvBase          Fields of the base file to make the join
     * @param fieldCsvToJoin        Fields of the ToJoin file to make the join. If multiple, will be seeked from the same range of the table.
     * @param addNoCorrespondance   If correspondance between base and toJoin not found, do we still copy the base line ?
     * @param doNotAddDoubledFields does not copy fields if they are already present in the base fields (in case of doubled attributes)
     * @param fileOut               File where to write the output
     * @return the wrote new CSV file
     * @throws IOException Reading and writing files
     * @deprecated bugged     * @param csvBase               File that will be copied in a whole
     */
    public static File joinCSVs(File csvBase, File csvToJoin, String[] fieldCsvBase, String[] fieldCsvToJoin, boolean addNoCorrespondance, boolean doNotAddDoubledFields, File fileOut) throws IOException {
        CSVReader r = getCSVReader(csvToJoin);
        String[] fLine = r.readNext();
        List<String> listFieldsToJoin = new ArrayList<>();
        for (String s : fLine)
            if (!Arrays.asList(fieldCsvToJoin).contains(s)) // don't add joining fields as we don't want to add them double
                listFieldsToJoin.add(s);
        return joinCSVs(csvBase, csvToJoin, fieldCsvBase, fieldCsvToJoin, addNoCorrespondance, listFieldsToJoin, doNotAddDoubledFields, fileOut);
    }

//    public static void main(String[] args) throws IOException {
//        Csv.sep = ';';
//        File menIndifCSV = CsvOp.joinCSVs(new File("/home/mc/Nextcloud/boulot/inria/privee/EGT/lil-0883.csv/Csv/menages_dimanche.csv"),
//                new File("/home/mc/Nextcloud/boulot/inria/privee/EGT/lil-0883.csv/Csv/personnes_dimanche.csv"),
//                new String[]{"NQUEST"}, new String[]{"NQUEST"}, true, true, new File("/tmp/MenIndiv.csv"));
//        File menIndivDeplCSV = CsvOp.joinCSVs(menIndifCSV,
//                new File("/home/mc/Nextcloud/boulot/inria/privee/EGT/lil-0883.csv/Csv/deplacements_dimanche.csv"),
//                new String[]{"NQUEST", "NP"}, new String[]{"NQUEST", "NP"}, true, true, new File("/tmp/MenIndivDepl.csv"));
//        CsvOp.joinCSVs(menIndivDeplCSV,
//                new File("/home/mc/Nextcloud/boulot/inria/privee/EGT/lil-0883.csv/Csv/trajets_dimanche.csv"),
//                new String[]{"NQUEST", "NP", "ND"}, new String[]{"NQUEST", "NP", "ND"}, true, true, new File("/tmp/MenIndivDeplTraj.csv"));
//
//    }

    /**
     * Join to CSV by searching common values in predefined fields. The whole line of the <i>base</i> csv is copied and only the precised fields of the <i>toJoin</i> .csv are copied.
     *
     * @param csvBase               File that will be copied in a whole
     * @param csvToJoin             File to join
     * @param fieldsCsvBase         Fields of the base file to make the join
     * @param fieldsCsvToJoin       Fields of the ToJoin file to make the join. If multiple, will be seeked from the same range of the table.
     * @param addNoCorrespondance   If correspondance between base and toJoin not found, do we still copy the base line ?
     * @param listFieldsToJoin      List of fields from the ToJoin file to copy
     * @param doNotAddDoubledFields does not copy fields if they are already present in the base fields (in case of doubled attributes)
     * @param fileOut               File where to write the output
     * @return the wrote new CSV file
     * @throws IOException Reading and writing files
     * @deprecated bugged
     */
    public static File joinCSVs(File csvBase, File csvToJoin, String[] fieldsCsvBase, String[] fieldsCsvToJoin, boolean addNoCorrespondance, List<String> listFieldsToJoin, boolean doNotAddDoubledFields, File fileOut) throws IOException {
        if (!csvBase.exists() || !csvToJoin.exists()) {
            System.out.println("joinCSVs: One of the files doesn't exist");
            return null;
        }
        if (fieldsCsvBase.length != fieldsCsvToJoin.length) {
            System.out.println("joinCSVs: tabs of the attributes to match are not the same length");
            return null;
        }

        CSVReader rB = getCSVReader(csvBase);
        CSVReader rTJ = getCSVReader(csvToJoin);
        String[] fLineCsvBase = rB.readNext();
        String[] fLineCsvToJoin = rTJ.readNext();
        int[] iFiledBase = new int[fieldsCsvBase.length];
        int[] iFiledToJoin = new int[fieldsCsvToJoin.length];
        List<Integer> listIFieldsToJoin = new ArrayList<>();
        // list indices
        try {
            for (int i = 0; i < fieldsCsvBase.length; i++) {
                iFiledBase[i] = Attribute.getIndice(fLineCsvBase, fieldsCsvBase[i]);
                iFiledToJoin[i] = Attribute.getIndice(fLineCsvToJoin, fieldsCsvToJoin[i]);
            }
            for (String s : listFieldsToJoin)
                if (!doNotAddDoubledFields || !listIFieldsToJoin.contains(Attribute.getIndice(fLineCsvToJoin, s)))
                    listIFieldsToJoin.add(Attribute.getIndice(fLineCsvToJoin, s));
        } catch (FileNotFoundException f) {
            System.out.println("joinCSVs: One of the attribute is not in the CSV's header");
            return null;
        }
        CSVWriter w = getCSVWriter(fileOut);
        //Write first line
        String[] newFLine = new String[fLineCsvBase.length + listIFieldsToJoin.size()];
        int fLineCsvBaseLength = fLineCsvBase.length;
        // Fill new line with baseline infos
        System.arraycopy(fLineCsvBase, 0, newFLine, 0, fLineCsvBaseLength);
        fLineCsvBaseLength--;
        for (int indiceTJ : listIFieldsToJoin)
            newFLine[fLineCsvBaseLength++] = fLineCsvToJoin[indiceTJ];
        w.writeNext(newFLine);
        for (String[] lineBase : rB.readAll()) {
            boolean lineFound = true;
            for (String[] lineTJ : rTJ.readAll()) {
                for (int i = 0; i < fieldsCsvBase.length; i++) {
                    if (!lineBase[iFiledBase[i]].equals(lineTJ[iFiledToJoin[i]])) {
                        lineFound = false;
                        break;
                    } else
                        lineFound = true;
                }
                if (lineFound) {
                    String[] newLine = new String[lineBase.length + listIFieldsToJoin.size()];
                    int lLength = lineBase.length;
                    // Fill new line with baseline infos
                    System.arraycopy(lineBase, 0, newLine, 0, lLength);
                    lLength--; //to catch up with the next ++
                    for (int indiceTJ : listIFieldsToJoin)
                        newLine[lLength++] = lineTJ[indiceTJ];
                    w.writeNext(newLine);
                }
            }
            if (!lineFound && addNoCorrespondance)
                w.writeNext(lineBase);
            rTJ.close();
            rTJ = getCSVReader(csvToJoin);
            rTJ.readNext();
        }
        rB.close();
        rTJ.close();
        w.close();
        return fileOut;
    }

    /**
     * Get only the wanted column of a .csv. Could easily be overload to select multiple column.
     *
     * @param csvFile         Input .csv
     * @param outFile         Output .csv
     * @param columnName      Name of the column to isolate
     * @param keepFirstColumn Do we keep the first column, that could contain information, or not.
     * @return the output file
     * @throws IOException reading and writing
     */
    public static File isolateColumn(File csvFile, File outFile, String columnName, boolean keepFirstColumn) throws IOException {
        CSVReader r = Csv.getCSVReader(csvFile);
        CSVWriter w = Csv.getCSVWriter(outFile);
        String[] fLine = r.readNext();
        int iToKeep = Attribute.getIndice(fLine, columnName);
        if (keepFirstColumn)
            w.writeNext(new String[]{fLine[0], fLine[iToKeep]});
        else
            w.writeNext(new String[]{fLine[iToKeep]});
        for (String[] line : r.readAll())
            if (keepFirstColumn)
                w.writeNext(new String[]{line[0], line[iToKeep]});
            else
                w.writeNext(new String[]{line[iToKeep]});

        r.close();
        w.close();
        return outFile;
    }

    /**
     * Change the first column of a .csv file with a new line by re-writing. Input and output files must be different
     *
     * @param csvFile  input .csv file
     * @param outFile  output .csv file
     * @param newFLine new header for the .csv
     * @return output file
     * @throws IOException reading and writing
     */
    public static File renameFirstColumn(File csvFile, File outFile, String[] newFLine) throws IOException {
        CSVReader r = Csv.getCSVReader(csvFile);
        CSVWriter w = Csv.getCSVWriter(outFile);
        String[] fLine = r.readNext();
        if (csvFile == outFile)
            System.out.println("renameFirstColumn: input and output files are the same. That could be smart but unfortunately function is not ready for it");
        if (fLine.length != newFLine.length)
            System.out.println("renameFirstColumn: different length between original and new first line");
        w.writeNext(newFLine);
        for (String[] line : r.readAll())
            w.writeNext(line);
        r.close();
        w.close();
        return outFile;
    }

    /**
     * Filter lines from a .csv file by comparing values of a field to a given fixed values
     *
     * @param csvFile          .csv file
     * @param outFile          file to write the output
     * @param fieldNameFilter  name of the field for comparison
     * @param fieldValueFilter value of the field for which to compare the #fieldNameFilter values
     * @return the wrote #outFile
     * @throws IOException reading and writing
     */
    public static File filterCSV(File csvFile, File outFile, String fieldNameFilter, String fieldValueFilter) throws IOException {
        return filterCSV(csvFile, outFile, fieldNameFilter, fieldValueFilter, "equals");
    }

    /**
     * Filter lines from a .csv file by comparing values of a field to a given fixed values
     *
     * @param csvFile          .csv file
     * @param outFile          file to write the output
     * @param fieldNameFilter  name of the field for comparison
     * @param fieldValueFilter value of the field for which to compare the #fieldNameFilter values
     * @param op               Kind of operation to make on the strings. Could be :
     *                         <ul>
     *                         <li><b>startsWith</b>: beginning of the string must match</li>
     *                         <li><b>endsWith</b>: end of the string must match</li>
     *                         <li><b>contains</b>: some place of the string must match</li>
     *                         <li><b>any other key</b>: strings must be equals</li>
     *                         </ul>
     * @return the wrote #outFile
     * @throws IOException reading and writing
     */
    public static File filterCSV(File csvFile, File outFile, String fieldNameFilter, String fieldValueFilter, String op) throws IOException {
        CSVReader r = Csv.getCSVReader(csvFile);
        CSVWriter w = Csv.getCSVWriter(outFile);
        String[] fLine = r.readNext();
        w.writeNext(fLine);
        int i = Attribute.getIndice(fLine, fieldNameFilter);
        Iterator<String[]> it = r.iterator();
        while (it.hasNext()) {
            String[] line = it.next();
            switch (op) {
                case "startsWith":
                    if (line[i].startsWith(fieldValueFilter))
                        w.writeNext(line);
                    break;
                case "endsWith":
                    if (line[i].endsWith(fieldValueFilter))
                        w.writeNext(line);
                    break;
                case "contains":
                    if (line[i].contains(fieldValueFilter))
                        w.writeNext(line);
                    break;
                case "equals":
                    if (line[i].equals(fieldValueFilter))
                        w.writeNext(line);
                    break;
            }
        }
        r.close();
        w.close();
        return outFile;
    }

//    public static void main(String[] args) throws IOException {
//        Csv.sep = ';';
//        File last = CsvOp.sumCSVLine(
//                CsvOp.filterCSV(
//                        CsvOp.filterCSV(new File("/home/mc/Téléchargements/fr-esr-atlas_regional-effectifs-d-etudiants-inscrits.csv"), new File("/tmp/etu.csv"), "rentree", "2018"), new File("/tmp/paris5.csv"), "geo_nom", "Paris  5e"),
//                new File("/tmp/effec.csv"), "effectifhdccpge", new String[]{"geo_nom", "regroupement"}, Arrays.asList("geo_nom", "regroupement", "rgp_formations_ou_etablissements"));
//
//
//    }

    /**
     * This method will add the values of a given column of lines which given attributes are matching and keep the others designated values.
     * This is originally made to aggregate values of a tab (i.e, file is detailing counting values for man and woman and we want to agregate them).
     *
     * @param csvFile                input csv file
     * @param outFile                output csv file
     * @param fieldNameToConcatenate name of the field to sum up values
     * @param fieldValueToCompare    name of the fields to concatenate in order to compare which line is equal to one-another
     * @param listFieldsToJoin       name of the fields to add in the result. #fieldValueToCompare are not included.
     * @return corresponding .csv file
     * @throws IOException reading and writing .csv
     */
    public static File sumCSVLine(File csvFile, File outFile, String fieldNameToConcatenate, String[] fieldValueToCompare, List<String> listFieldsToJoin) throws IOException {
        CSVReader r = Csv.getCSVReader(csvFile);
        String[] fLine = r.readNext();
        // list indices
        int iFieldToConcat = Attribute.getIndice(fLine, fieldNameToConcatenate);
        Integer[] iFieldToCompare = new Integer[fieldValueToCompare.length];
        List<Integer> listIFieldsToJoin = new ArrayList<>();
        try {
            for (int i = 0; i < fieldValueToCompare.length; i++)
                iFieldToCompare[i] = Attribute.getIndice(fLine, fieldValueToCompare[i]);
            for (String s : listFieldsToJoin)
                listIFieldsToJoin.add(Attribute.getIndice(fLine, s));
        } catch (FileNotFoundException f) {
            System.out.println("joinCSVs: One of the attribute is not in the CSV's header");
            return null;
        }
        CSVWriter w = getCSVWriter(outFile);

        //create new first line
        String[] newFLine = new String[listIFieldsToJoin.size() + 1];
        int j = 0;
        for (int i = 0; i < fLine.length; i++)
            if (listIFieldsToJoin.contains(i))
                newFLine[j++] = fLine[i];
        newFLine[listIFieldsToJoin.size()] = fieldNameToConcatenate;
        w.writeNext(newFLine);
        List<String[]> linesToAdd = new ArrayList<>();
        HashMap<String, Integer> idsToAdd = new HashMap<>();

        //iterate over values and sum
        Iterator<String[]> it = r.iterator();
        while (it.hasNext()) {
            String[] line = it.next();
            //id creation
            String id = constructID(iFieldToCompare, line);
            //make a new line
            j = 0;
            String[] newLine = new String[newFLine.length];
            for (int i = 0; i < line.length; i++)
                if (listIFieldsToJoin.contains(i))
                    newLine[j++] = line[i];
            //search for the same values
            if (!line[iFieldToConcat].equals("")) { // if the field to concat has no value, we don't add it
                if (idsToAdd.containsKey(id))
                    idsToAdd.replace(id, idsToAdd.get(id) + Integer.parseInt(line[iFieldToConcat]));
                else {
                    idsToAdd.put(id, Integer.parseInt(line[iFieldToConcat]));
                    if (!linesToAdd.contains(newLine))
                        linesToAdd.add(newLine);
                }
            }
        }
        for (String[] line : linesToAdd) {
//            line[line.length - 1] = String.valueOf((line[line.length - 1] != null ? Integer.parseInt(line[line.length - 1]) : 0) + linesToAdd.get(line));
            // construct fields to compare for new line
            List<Integer> newIFieldToJoin = new ArrayList<>();
            for (String s : fieldValueToCompare)
                newIFieldToJoin.add(Attribute.getIndice(newFLine, s));
            line[line.length - 1] = String.valueOf(idsToAdd.get(constructID(newIFieldToJoin.toArray(Integer[]::new), line)));
            w.writeNext(line);
        }
        r.close();
        w.close();
        return outFile;
    }

    private static String constructID(Integer[] iFieldToCompare, String[] line) {
        StringBuilder idb = new StringBuilder();
        for (int i : iFieldToCompare) {
            idb.append(line[i]);
            idb.append(Csv.sep);
        }
        return idb.substring(0, idb.toString().length() - 1);
    }
}
