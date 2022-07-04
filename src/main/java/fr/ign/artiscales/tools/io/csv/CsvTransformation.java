package fr.ign.artiscales.tools.io.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class CsvTransformation extends Csv {
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
     * Get only the wanted column of a .csv. Could easily be overload to select multiple column.
     *
     * @param csvFile     Input .csv
     * @param outFile     Output .csv
     * @param columnsName Name of columns to isolate
     * @return the output file
     * @throws IOException reading and writing
     */
    public static File isolateColumns(File csvFile, File outFile, List<String> columnsName) throws IOException {
        CSVReader r = Csv.getCSVReader(csvFile);
        CSVWriter w = Csv.getCSVWriter(outFile);
        String[] fLine = r.readNext();
//        List<Integer> iToKeep = columnsName.stream().mapToInt(name -> Attribute.getIndice(fLine, name)).collect(Collectors.toList());
        List<Integer> iToKeep = new ArrayList<>();
        for (String name : columnsName)
            iToKeep.add(Attribute.getIndice(fLine, name));
        int i = 0;
        String[] newFLine = new String[iToKeep.size()];
        for (int ii : iToKeep) {
            newFLine[i++] = fLine[ii];
        }
        w.writeNext(newFLine);
        for (String[] line : r.readAll()) { //there might be better ways to do that
            String[] newLine = new String[iToKeep.size()];
            int j = 0;
            for (int ii : iToKeep)
                newLine[j++] = line[ii];
            w.writeNext(newLine);
        }
        r.close();
        w.close();
        return outFile;
    }

    /**
     * Change the header of a .csv file with a new line by re-writing. Input and output files must be different.
     *
     * @param inCsvFile input .csv file
     * @param outFile   output .csv file
     * @param newFLine  new header for the .csv
     * @return output file
     * @throws IOException reading and writing
     */
    public static File renameFirstLine(File inCsvFile, File outFile, String[] newFLine) throws IOException {
        CSVReader r = Csv.getCSVReader(inCsvFile);
        CSVWriter w = Csv.getCSVWriter(outFile);
        String[] fLine = r.readNext();
        if (inCsvFile == outFile)
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
        return filterCSV(csvFile, outFile, fieldNameFilter, List.of(fieldValueFilter), op);
    }

    /**
     * Filter lines from a .csv file by comparing values of a field to a given fixed values
     *
     * @param csvFile           .csv file
     * @param outFile           file to write the output
     * @param fieldNameFilter   name of the field for comparison
     * @param fieldValuesFilter values of the field for which to compare the #fieldNameFilter values
     * @param op                Kind of operation to make on the strings. Could be :
     *                          <ul>
     *                          <li><b>startsWith</b>: beginning of the string must match</li>
     *                          <li><b>endsWith</b>: end of the string must match</li>
     *                          <li><b>contains</b>: some place of the string must match</li>
     *                          <li><b>any other key</b>: strings must be equals</li>
     *                          </ul>
     * @return the wrote #outFile
     * @throws IOException reading and writing
     */
    public static File filterCSV(File csvFile, File outFile, String fieldNameFilter, List<String> fieldValuesFilter, String op) throws IOException {
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
                    for (String fieldValueFilter : fieldValuesFilter)
                        if (line[i].startsWith(fieldValueFilter)) {
                            w.writeNext(line);
                            break;
                        }
                    break;
                case "endsWith":
                    for (String fieldValueFilter : fieldValuesFilter)
                        if (line[i].endsWith(fieldValueFilter)) {
                            w.writeNext(line);
                            break;
                        }
                    break;
                case "contains":
                    for (String fieldValueFilter : fieldValuesFilter)
                        if (line[i].contains(fieldValueFilter)) {
                            w.writeNext(line);
                            break;
                        }
                    break;
                case "equals":
                    for (String fieldValueFilter : fieldValuesFilter)
                        if (line[i].equals(fieldValueFilter)) {
                            w.writeNext(line);
                            break;
                        }
                    break;
            }
        }
        r.close();
        w.close();
        return outFile;
    }

    public static File replaceValueInCSV(File csvFile, File outFile, String fieldNameFilter, String fieldValueToReplace, String replacement) throws IOException {
        CSVReader r = Csv.getCSVReader(csvFile);
        CSVWriter w = Csv.getCSVWriter(outFile);
        String[] fLine = r.readNext();
        w.writeNext(fLine);
        int i = Attribute.getIndice(fLine, fieldNameFilter);
        Iterator<String[]> it = r.iterator();
        while (it.hasNext()) {
            String[] line = it.next();
            if (line[i].equals(fieldValueToReplace))
                line[i] = replacement;
            w.writeNext(line);
        }
        r.close();
        w.close();
        return outFile;
    }

//    public static void main(String[] args) throws IOException {
//        SortedMap<String, Boolean> hm = new TreeMap<>();
//        hm.put("val", true);
//        sortCsv(new File("/home/mc/Documents/in.csv"),new File("/home/mc/Documents//out.csv"), hm);
//    }

//    /**
//     * This sort a csv relatively to one or multiple field values.
//     * Load everything into memory -> method doesn't fit if
//     *
//     * @param inputCsv                     input .csv file. Must contain a first line.
//     * @param outputCsv                    output .csv file
//     * @param attributeNamesNaturalOrdered this contains every fields to sort the .csv with and if it has to be done respecting their natural order or not. First element of the collection will be sorted first.
//     * @return the wrote .csv file
//     */
//    public static File sortCsv(File inputCsv, File outputCsv, SortedMap<String, Boolean> attributeNamesNaturalOrdered) throws IOException {
//        CSVReader r = Csv.getCSVReader(inputCsv);
//        String[] firstLine = r.readNext();
//        List<String[]> values = r.readAll();
//        for (String attr : attributeNamesNaturalOrdered.keySet()) {
//            int iAttToSort = Attribute.getIndice(r.readNext(), attr);
//
//        }
//
//    }

}
