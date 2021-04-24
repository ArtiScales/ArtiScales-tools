package fr.ign.artiscales.tools.io.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvOp {

    /**
     * Get a list of unique values for a given field from a .csv file
     *
     * @param csvFile   csv file
     * @param fieldName name of the field to sum values
     * @return a list of unique values
     */
    public static List<String> getUniqueFieldValue(File csvFile, String fieldName) throws IOException {
        CSVReader r = new CSVReader(new FileReader(csvFile));
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
        CSVReader r = new CSVReader(new FileReader(csvFile));
        String[] fLine = r.readNext();
        int iTarget = Attribute.getIndice(fLine, targetAttributeName);
        int iWanted = Attribute.getIndice(fLine, wantedAttributeName);
        List<String> result = new ArrayList<>();
        for (String[] line : r.readAll())
            if (line[iTarget].equals(targetAttributeValue))
                result.add(line[iWanted]);
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
        CSVReader r = new CSVReader(new FileReader(csvFile));
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
     * Get the indice number on the position of the header of a .csv file
     *
     * @param csvFile   .csv file with a header
     * @param fieldName name of the field to get indice from
     * @return the indice on which number
     */
    public static int getIndice(File csvFile, String fieldName) throws IOException {
        CSVReader r = new CSVReader(new FileReader(csvFile));
        int i = Attribute.getIndice(r.readNext(), fieldName);
        r.close();
        return i;
    }


    /**
     * Add some basic statistic for a predeterminde column of a csv file. Add extra lines to the end of the .csv.
     *
     * @param csvFile    path to the .csv {@link File}
     * @param nbCol      number of the column to calulate stats (begins at 0)
     * @param isFirstCol true if there's a header and it will be ignored
     * @throws IOException
     */
    public static void calculateColumnsBasicStat(File csvFile, int nbCol, boolean isFirstCol) throws IOException {
        CSVReader r = new CSVReader(new FileReader(csvFile));
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
        CSVWriter w = new CSVWriter(new FileWriter(csvFile));
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
     * @param csvBase             File that will be copied in a whole
     * @param csvToJoin           File to join
     * @param fieldCsvBase        Filed of the base file to make the join
     * @param fieldCsvToJoin      Filed of the ToJoin file to make the join
     * @param addNoCorrespondance If correspondance between base and toJoin not found, do we still copy the base line ?
     * @param listFieldsToJoin    List of fields from the ToJoin file to copy
     * @param fileOut             File where to write the output
     * @return the wrote new CSV file
     * @throws IOException Reading and writing files
     */
    public static File joinCSVs(File csvBase, File csvToJoin, String fieldCsvBase, String fieldCsvToJoin, boolean addNoCorrespondance, List<String> listFieldsToJoin, File fileOut) throws IOException {
        if (!csvBase.exists() || !csvToJoin.exists()) {
            System.out.println("joinCSVs: One of the files doesn't exist");
            return null;
        }
        CSVReader rB = new CSVReader(new FileReader(csvBase));
        CSVReader rTJ = new CSVReader(new FileReader(csvToJoin));
        String[] fLineCsvBase = rB.readNext();
        String[] fLineCsvToJoin = rTJ.readNext();
        int iFiledBase;
        int iFiledToJoin;
        List<Integer> listIFieldsToJoin = new ArrayList<>();
        try {
            iFiledBase = Attribute.getIndice(fLineCsvBase, fieldCsvBase);
            iFiledToJoin = Attribute.getIndice(fLineCsvToJoin, fieldCsvToJoin);
            for (String s : listFieldsToJoin)
                listIFieldsToJoin.add(Attribute.getIndice(fLineCsvToJoin, s));
        } catch (FileNotFoundException f) {
            System.out.println("joinCSVs: One of the attribute is not in the CSV's header");
            return null;
        }

        CSVWriter w = new CSVWriter(new FileWriter(fileOut));
        //Write first line
        String[] newFLine = new String[fLineCsvBase.length + listFieldsToJoin.size()];
        int fLineCsvBaseLength = fLineCsvBase.length;
        // Fill new line with baseline infos
        System.arraycopy(fLineCsvBase, 0, newFLine, 0, fLineCsvBaseLength);
        fLineCsvBaseLength--;
        for (int indiceTJ : listIFieldsToJoin)
            newFLine[fLineCsvBaseLength++] = fLineCsvToJoin[indiceTJ];
        w.writeNext(newFLine);

        for (String[] line : rB.readAll()) {
            boolean lineFound = false;
            for (String[] lineTJ : rTJ.readAll()) {
                if (line[iFiledBase].equals(lineTJ[iFiledToJoin])) {
                    String[] newLine = new String[line.length + listFieldsToJoin.size()];
                    int lLength = line.length;
                    // Fill new line with baseline infos
                    System.arraycopy(line, 0, newLine, 0, lLength);
                    lLength--; //to catch up with the next ++
                    for (int indiceTJ : listIFieldsToJoin)
                        newLine[lLength++] = lineTJ[indiceTJ];
                    w.writeNext(newLine);
                    lineFound = true;
                    break;
                }
            }
            if (!lineFound && addNoCorrespondance)
                w.writeNext(line);
            rTJ.close();
            rTJ = new CSVReader(new FileReader(csvToJoin));
            rTJ.readNext();
        }
        rB.close();
        rTJ.close();
        w.close();
        return fileOut;
    }

}
