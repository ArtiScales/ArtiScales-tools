package fr.ign.artiscales.tools.io;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * Class that contains multiple methods to manipulate a .csv file
 *
 * @author Maxime Colomb
 */
public class Csv {

    public static boolean needFLine = true;

//    public static void main(String[] args) throws IOException {
//        mergeCsvFilesCol(Arrays.asList(new File("/tmp/age-80 - P.tif-tocsv.csv"), new File("/tmp/sex-Homme.tif-tocsv.csv"), new File("/tmp/resto.tif-tocsv.csv")), new File("/tmp/"), "merged.csv", true);
//    }

    /**
     * Get the indice number on the position of the header of a .csv file
     *
     * @param csvFile .csv file with a header
     * @return the indice on which number
     */
    public static int getIndice(File csvFile, String attributeName) throws IOException {
        CSVReader r = new CSVReader(new FileReader(csvFile));
        int i = Attribute.getIndice(r.readNext(), attributeName);
        r.close();
        return i;
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
     * Merge every .csv file contained into a folder and its subfolders with a recursive method. Must have the same header.
     *
     * @param rootFolder
     * @param outFile
     * @throws IOException
     */
    public static File mergeCSVFiles(File rootFolder, File outFile) throws IOException {
        if (outFile.exists())
            Files.delete(outFile.toPath());
        List<File> listCSV = getCSVFiles(rootFolder);
        return mergeCSVFiles(listCSV, outFile);
    }

    public static List<File> getCSVFiles(File folder) {
        List<File> result = new ArrayList<>();
        for (File f : Objects.requireNonNull(folder.listFiles()))
            if (f.isDirectory())
                result.addAll(getCSVFiles(f));
            else if (f.getName().endsWith(".csv"))
                result.add(f);
        return result;
    }

    /**
     * Merge a given list of .csv files. Only the first header is pasted (and all files must have one).
     *
     * @param filesToMerge
     * @param outFile
     * @return the outFile param where a merged .csv file should have been generated
     * @throws IOException
     */
    public static File mergeCSVFiles(List<File> filesToMerge, File outFile) throws IOException {
        CSVReader defCsv = new CSVReader(new FileReader(filesToMerge.get(0)));
        String[] firstLineDef = defCsv.readNext();
        defCsv.close();
        CSVWriter output = new CSVWriter(new FileWriter(outFile, true));
        output.writeNext(firstLineDef);
        for (File fileToMerge : filesToMerge) {
            CSVReader ptCsv = new CSVReader(new FileReader(fileToMerge));
            String[] firstLine = ptCsv.readNext();
            if (firstLine.length != firstLineDef.length)
                System.out.println("mergeCSVFiles() method merges different files");
            for (String[] line : ptCsv)
                output.writeNext(line);
            ptCsv.close();
        }
        output.close();
        return outFile;
    }

    /**
     * Generates a .csv file out of a {@link HashMap}. Used mainly for the MUP-City particular evaluation analysis
     *
     * @param results
     * @param folderOut
     * @param name
     * @throws IOException
     */
    public static File generateCsvFileMultTab(HashMap<String, HashMap<String, Double[]>> results, String name, String firstLine, File folderOut)
            throws IOException {
        File fileName = new File(folderOut + "/" + name + ".csv");
        FileWriter writer = new FileWriter(fileName, true);
        for (String tab : results.keySet()) {
            HashMap<String, Double[]> intResult = results.get(tab);
            if (needFLine) {
                writer.append("scenario ").append(tab).append(",").append(firstLine).append("\n");
                needFLine = false;
            }
            for (String nomScenar : intResult.keySet()) {
                writer.append(nomScenar).append(",").append(String.valueOf(intResult.get(nomScenar)[0])).append(",").append(String.valueOf(intResult.get(nomScenar)[1]));
                writer.append("\n");
            }
            writer.append("\n");
        }
        writer.close();
        return fileName;
    }

    /**
     * Generates a .csv file out of a {@link HashMap}. Used mainly for the MUP-City analysis objects RasterMergeResult
     *
     * @param results
     * @param folderOut
     * @param name
     * @throws IOException
     */
    public static File generateCsvFileMultTab(HashMap<String, HashMap<String, Double>> results, File folderOut, String name) throws IOException {
        File fileName = new File(folderOut + "/" + name + ".csv");
        FileWriter writer = new FileWriter(fileName, true);
        for (String tab : results.keySet()) {
            HashMap<String, Double> intResult = results.get(tab);
            writer.append("scenario ").append(tab).append("\n");
            for (String nomScenar : intResult.keySet()) {
                writer.append(nomScenar).append(",").append(String.valueOf(intResult.get(nomScenar)));
                writer.append("\n");
            }
            writer.append("\n");
        }
        writer.close();
        return fileName;
    }

    /**
     * This method is supposed to generate latex tabs but not real sure that works @TODO to test
     *
     * @param results
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @throws IOException
     */
    public static File generateLatexMultTab(HashMap<String, HashMap<String, Double>> results, File folderOut, String name) throws IOException {
        File fileName = new File(folderOut + "/" + name + ".txt");
        FileWriter writer = new FileWriter(fileName, true);
        for (String tab : results.keySet()) {
            HashMap<String, Double> intResult = results.get(tab);
            writer.append("scenario ").append(tab).append("\n");
            for (String nomScenar : intResult.keySet()) {
                writer.append(nomScenar).append("&").append(String.valueOf(intResult.get(nomScenar)));
                writer.append("\n");
            }
            writer.append("\n");
        }
        writer.close();
        return fileName;
    }

    /**
     * Generate a .csv file out of a hashtable. Each key is the first entry of a new line. By default, if an already existing .csv file exists, the new data are append to it
     *
     * @param cellRepet Values are a double table (they are then converted in Object[])
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @param firstCol  Header of the .csv file (can be null)
     * @throws IOException
     */
    public static File generateCsvFile(HashMap<String, double[]> cellRepet, File folderOut, String name, String[] firstCol) throws IOException {
        return generateCsvFile(cellRepet, name, folderOut, firstCol, true);
    }

    /**
     * generate a .csv file out of a hashtable. Each key is the first entry of a new line
     *
     * @param data      Values are a double table (they are then converted in Object[])
     * @param name      name of the .csv file
     * @param folderOut folder where the .csv file is created
     * @param firstCol  header of the .csv file (can be null)
     * @param append    in the case an already existing .csv file exists: if true, the new data are append to it. If false, the new table overwritte the old one.
     * @throws IOException
     */
    public static File generateCsvFile(HashMap<String, double[]> data, String name, File folderOut, String[] firstCol, boolean append)
            throws IOException {
        HashMap<String, Object[]> result = new HashMap<>();
        for (String key : data.keySet()) {
            double[] line = data.get(key);
            Object[] toPut = new Object[line.length];
            for (int i = 0; i < line.length; i++)
                toPut[i] = line[i];
            result.put(key, toPut);
        }
        return generateCsvFile(result, folderOut, name, firstCol, append);
    }

    /**
     * Generate a .csv file out of a hashtable. Each key is the first entry of a new line
     *
     * @param data      Values are a String table (they are then converted in Object[])
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @param append    In the case an already existing .csv file exists: if true, the new data are append to it. If false, the new table overwritte the old one.
     * @param firstCol  Header of the .csv file (can be null)
     * @throws IOException
     */
    public static File generateCsvFile(HashMap<String, String[]> data, File folderOut, String name, boolean append, String[] firstCol)
            throws IOException {
        HashMap<String, Object[]> result = new HashMap<>();
        for (String key : data.keySet()) {
            String[] line = data.get(key);
            Object[] toPut = new Object[line.length];
            System.arraycopy(line, 0, toPut, 0, line.length);
            result.put(key, toPut);
        }
        return generateCsvFile(result, folderOut, name, firstCol, append);
    }

    /**
     * Generate a .csv file out of a hashtable. Each key is the first entry of a new line. By default, if an already existing .csv file exists, the new data are append to it
     *
     * @param cellRepet Values are an Object table
     * @param name      Name of the .csv file
     * @param folderOut Folder where the .csv file is created
     * @param firstCol  Header of the .csv file (can be null)
     * @throws IOException
     */
    public static File generateCsvFile(HashMap<String, Object[]> cellRepet, String name, File folderOut, String[] firstCol) throws IOException {
        return generateCsvFile(cellRepet, folderOut, name, firstCol, true);
    }

    public static File generateCsvFile(String name, File folderOut, String[] firstCol, boolean append, HashMap<String, ?> cellRepet) throws IOException {
        HashMap<String, Object[]> cellRepetArrayed = new HashMap<>();
        for (String c : cellRepet.keySet()) {
            Object[] a = {cellRepet.get(c)};
            cellRepetArrayed.put(c, a);
        }
        return generateCsvFile(cellRepetArrayed, folderOut, name, firstCol, append);
    }

    /**
     * Generate a .csv file out of a hashtable. Each key is the first entry of a new line.
     *
     * @param cellRepet Values are an Object table
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @param firstCol  Header of the .csv file (can be null)
     * @param append    In the case an already existing .csv file exists: if true, the new data are append to it. If false, the new table overwritte the old one.
     * @return The exported File
     * @throws IOException
     */
    public static File generateCsvFile(HashMap<String, Object[]> cellRepet, File folderOut, String name, String[] firstCol, boolean append)
            throws IOException {
        StringBuilder fLine = new StringBuilder();
        if (firstCol != null) {
            fLine = new StringBuilder(firstCol[0]);
            for (int i = 1; i < firstCol.length; i++)
                fLine.append(",").append(firstCol[i]);
        }
        List<String> lines = new ArrayList<>();
        for (String nom : cellRepet.keySet()) {
            StringBuilder line = new StringBuilder(nom + ",");
            for (Object val : cellRepet.get(nom))
                line.append(val).append(",");
            if (line.toString().endsWith(","))
                line = new StringBuilder(line.substring(0, line.length() - 1));
            lines.add(line.toString());
        }
        return simpleCSVWriter(lines, fLine.toString(), new File(folderOut, name + ".csv"), append);
    }

    /**
     * Generate a .csv file out of a hashtable. Data are dispalyed in colomn and each key is placed in the header
     *
     * @param cellRepet Values are a double table (they are then converted in Object[])
     * @param name      name of the .csv file
     * @param folderOut folder where the .csv file is created
     * @throws IOException
     */
    public static File generateCsvFileCol(HashMap<String, double[]> cellRepet, String name, File folderOut) throws IOException {
        HashMap<String, Object[]> result = new HashMap<>();
        for (double[] ligne : cellRepet.values()) {
            Object[] aMettre = new Object[ligne.length - 1];
            for (int i = 1; i < ligne.length; i = i + 1)
                aMettre[i - 1] = ligne[i];
            result.put(String.valueOf(ligne[0]), aMettre);
        }
        return generateCsvFileCol(result, folderOut, name);
    }

    /**
     * Merge every columns of multiple .csv files by their first column reference.
     *
     * @param listFiles list of files to merge
     * @param folderOut where to finally write the merged .csv
     * @param name      name of the merged file
     * @return merged .csv
     * @throws IOException
     */
    public static File mergeCsvFilesCol(List<File> listFiles, File folderOut, String name, boolean ordered) throws IOException {
        // test
        for (File f : listFiles)
            if (!f.getName().endsWith(".csv")) {
                System.out.println(f + " is not a .csv. Rename it if your sure it is. Return null");
                return null;
            }
        if (ordered) {
            // check that every files has the same line amount
            int length = (new CSVReader(new FileReader(listFiles.get(0)))).readAll().size();
            for (File f : listFiles)
                if ((new CSVReader(new FileReader(f))).readAll().size() != length)
                    System.out.println("mergeCsvFilesCol doesn't have the same size column. You may have a problem, tabs are unlikely to have the same order, and would prefer use the ordered=false argument");
            boolean coordinates = true;
            HashMap<String, Object[]> mergedCSV = new HashMap<>();
            for (File f : listFiles) {
                CSVReader csvr = new CSVReader(new FileReader(f));
                String[] attrNames = csvr.readNext();
                if (coordinates) {
                    Object[] obj = new Object[length];
                    int i = 0;
                    for (String[] l : csvr.readAll())
                        obj[i++] = l[0];
                    mergedCSV.put(String.valueOf(attrNames[0]), obj);
                    csvr = new CSVReader(new FileReader(f));
                    csvr.readNext();
                    coordinates = false;
                }
                for (int j = 1; j < attrNames.length; j++) {
                    Object[] obj = new Object[length];
                    int i = 0;
                    for (String[] l : csvr.readAll())
                        obj[i++] = l[j];
                    mergedCSV.put(String.valueOf(attrNames[j]), obj);
                }
            }
            return generateCsvFileCol(mergedCSV, folderOut, name);
        } else {
            System.out.println("Not ordered data's not implemented yet");
            return null;
        }
    }

    /**
     * Generate a .csv file out of a hashtable. Data are dispalyed in colomn and each key is placed in the header
     *
     * @param cellRepet Values are an Object table
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file * @throws IOException
     */
    public static File generateCsvFileCol(HashMap<String, Object[]> cellRepet, File folderOut, String name) throws IOException {
        File fileName = new File(folderOut + "/" + name + ".csv");
        FileWriter writer = new FileWriter(fileName, false);

        // selec the longest tab
        int longestTab = 0;
        for (Object[] tab : cellRepet.values()) {
            if (tab.length > longestTab)
                longestTab = tab.length;
        }
        // put the main names
        for (String nomm : cellRepet.keySet())
            writer.append(nomm).append(",");
        writer.append("\n");
        for (int i = 0; i <= longestTab - 1; i++) {
            for (String nomm : cellRepet.keySet()) {
                try {
                    writer.append(String.valueOf(cellRepet.get(nomm)[i])).append(",");
                } catch (ArrayIndexOutOfBoundsException a) {
                    writer.append(",");
                    // normal that it gets Arrays exceptions.
                }
            }
            writer.append("\n");
        }
        writer.close();
        return fileName;
    }

    public static File simpleCSVWriter(List<String> lines, String firstLine, File fileOut, boolean append) throws IOException {
        if (!fileOut.getName().endsWith(".csv"))
            fileOut = new File(fileOut + ".csv");
        fileOut.getParentFile().mkdirs();
        FileWriter writer = new FileWriter(fileOut, append);
        if (needFLine) {
            writer.append(firstLine).append("\n");
            needFLine = false;
        }
        for (String l : lines) {
            writer.append(l);
            writer.append("\n");
        }
        writer.close();
        return fileOut;
    }

    public static void simpleCSVWriter(List<String> lines, File f, boolean append) throws IOException {
        simpleCSVWriter(lines, "", f, append);
    }
}
