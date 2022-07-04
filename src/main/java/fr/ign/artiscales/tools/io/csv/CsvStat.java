package fr.ign.artiscales.tools.io.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class CsvStat extends Csv {

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
