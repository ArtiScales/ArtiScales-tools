package fr.ign.artiscales.tools.io.csv;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


/**
 * Class that contains multiple methods to manipulate a .csv file
 *
 * @author Maxime Colomb
 */
public class CsvExport extends Csv {

    public static boolean needFLine = true;

    /**
     * Export a {@link DescriptiveStatistics} ofject to a .csv tab
     *
     * @param ds      {@link DescriptiveStatistics} with values
     * @param outFile file to write in
     * @throws IOException writing file
     */
    public static void exportDescStatToCSV(DescriptiveStatistics ds, File outFile) throws IOException {
        List<String> lVal = new ArrayList<>();
        for (double val : ds.getValues())
            lVal.add(String.valueOf(val));
        simpleCSVWriter(lVal, outFile, false);
    }

    /**
     * Merge every .csv file contained into a folder and its subfolders with a recursive method. Must have the same header.
     *
     * @param rootFolder folder containing some .csvfiles
     * @param outFile    path to the merged file. Will be overwritten.
     * @return the written outFile
     * @throws IOException delete and writing or reading files
     */
    public static File mergeCSVFiles(File rootFolder, File outFile) throws IOException {
        if (outFile.exists())
            Files.delete(outFile.toPath());
        return mergeCSVFiles(getCSVFiles(rootFolder), outFile);
    }

    /**
     * Get every .csv files from a folder and it ssubfolder.
     * Uses a recursive method.
     *
     * @param folder root folder from where the serach comes from
     * @return the found list of .csv files
     */
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
     * Merge a given list of .csv files. Lines are added under next lines.
     * Only the first header is pasted (and all files must have one).
     *
     * @param filesToMerge list of .csv files to merge together. They assume to have the same header.
     * @param outFile      path to the merged file. Will be overwritten.
     * @return the written outFile
     * @throws IOException reading or writing file
     */
    public static File mergeCSVFiles(List<File> filesToMerge, File outFile) throws IOException {
        CSVReader defCsv = getCSVReader(filesToMerge.get(0));
        String[] firstLineDef = defCsv.readNext();
        defCsv.close();
        CSVWriter output = getCSVWriter(outFile, true);
        output.writeNext(firstLineDef);
        for (File fileToMerge : filesToMerge) {
            CSVReader ptCsv = getCSVReader(fileToMerge);
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
     * @param results   collection to write
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @throws IOException writing file
     */
    public static File generateCsvFileMultTab(HashMap<String, HashMap<String, Double[]>> results, String name, String firstLine, File folderOut) throws IOException {
        File fileName = new File(folderOut + "/" + name + (name.endsWith(".csv") ? "" : ".csv"));
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
     * @param results   data to write
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @throws IOException writing file
     */
    public static File generateCsvFileMultTab(HashMap<String, HashMap<String, Double>> results, File folderOut, String name) throws IOException {
        File fileName = new File(folderOut + "/" + name + (name.endsWith(".csv") ? "" : ".csv"));
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
     * @param results   collection to write
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @throws IOException writing file
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
     * @throws IOException writing file
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
     * @throws IOException writing file
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
     * @throws IOException writing file
     */
    public static File generateCsvFile(HashMap<String, String[]> data, File folderOut, String name, boolean append, String[] firstCol) throws IOException {
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
     * @throws IOException writing file
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
     * @throws IOException writing file
     */
    public static File generateCsvFile(HashMap<String, Object[]> cellRepet, File folderOut, String name, String[] firstCol, boolean append) throws IOException {
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
     * @throws IOException write file
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
     * Respect column orders (by file, then inside files)
     *
     * @param listFiles            list of files to merge
     * @param folderOut            where to finally write the merged .csv
     * @param name                 name of the merged file
     * @param columnNameValueToAdd pairs of name/values to add to each merged csv. Values will be constant and the order they are placed in the list must correspond to the merged files.
     *                             Column(s) are added before the .csv values.
     * @return merged .csv
     * @throws IOException write file
     */
    public static File mergeColumnsOfCsvFiles(List<File> listFiles, File folderOut, String name, Pair<String, LinkedList<String>>... columnNameValueToAdd) throws IOException {
        // test
        for (File f : listFiles)
            if (!f.getName().endsWith(".csv")) {
                System.out.println(f + " is not a .csv. Rename it if your sure it is. Return null");
                return null;
            }
        // check that every files has the same line amount
        int length = (getCSVReader(listFiles.get(0))).readAll().size();
        for (File f : listFiles)
            if (getCSVReader(f).readAll().size() != length) {
                System.out.println("mergeColumnsOfCsvFiles() doesn't have the same size column. You might have a problem in the merge");
                length = Math.max(getCSVReader(f).readAll().size(), length);
            }
        LinkedList<Object[]> mergedCSV = new LinkedList<>();
        for (int fi = 0; fi < listFiles.size(); fi++) {
            File f = listFiles.get(fi);
            CSVReader csvr = getCSVReader(f);
            String[] fLine = csvr.readNext();
            csvr.close();
            // add constant columns
            for (Pair<String, LinkedList<String>> pair : columnNameValueToAdd) {
                Object[] lineCol = new Object[length];
                lineCol[0] = pair.getKey();
                for (int k = 1; k < length; k++)
                    lineCol[k] = pair.getValue().get(fi);
                mergedCSV.add(lineCol);
            }
            //add .csv columns
            for (int i = 0; i < fLine.length; i++) {
                CSVReader csvrTemp = getCSVReader(f);
                Object[] lineCol = new Object[length];
                int j = 0;
                for (Object[] line : csvrTemp.readAll())
                    lineCol[j++] = line[i] == null ? "" : line[i];
                mergedCSV.add(lineCol);
                csvrTemp.close();
            }
        }
        return generateCsvFileCol(mergedCSV, folderOut, name);
    }

    /**
     * Generate a .csv file out of a hashtable. Data are dispalyed in colomn and each key is placed in the header
     *
     * @param cellRepet Values are an Object table
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @throws IOException write file
     */
    public static File generateCsvFileCol(HashMap<String, Object[]> cellRepet, File folderOut, String name) throws IOException {
        File fileName = new File(folderOut + "/" + name + (name.endsWith(".csv") ? "" : ".csv"));
        FileWriter writer = new FileWriter(fileName, false);
        // selec the longest tab
        int longestTab = 0;
        for (Object[] tab : cellRepet.values())
            if (tab.length > longestTab)
                longestTab = tab.length;
        // put the main names
        for (String nomm : cellRepet.keySet())
            writer.append(nomm).append(",");
        writer.append("\n");
        for (int i = 0; i <= longestTab - 1; i++) {
            for (String nomm : cellRepet.keySet())
                try {
                    writer.append(String.valueOf(cellRepet.get(nomm)[i])).append(",");
                } catch (ArrayIndexOutOfBoundsException a) {
                    writer.append(",");
                    // normal that it gets Arrays exceptions.
                }
            writer.append("\n");
        }
        writer.close();
        return fileName;
    }

    /**
     * Generate a .csv file out of a hashtable. Data are dispalyed in colomn and each key is placed in the header
     *
     * @param cellRepet Values are an Object table
     * @param folderOut Folder where the .csv file is created
     * @param name      Name of the .csv file
     * @throws IOException write file
     */
    public static File generateCsvFileCol(LinkedList<Object[]> cellRepet, File folderOut, String name) throws IOException {
        File fileName = new File(folderOut + "/" + name + (name.endsWith(".csv") ? "" : ".csv"));
        FileWriter writer = new FileWriter(fileName, false);
        // selec the longest tab
        int longestTab = 0;
        for (Object[] tab : cellRepet)
            if (tab.length > longestTab)
                longestTab = tab.length;
        for (int i = 0; i < longestTab; i++) {
            for (Object[] line : cellRepet)
                try {
                    writer.append(String.valueOf(line[i])).append(",");
                } catch (ArrayIndexOutOfBoundsException a) { // normal that it gets Arrays exceptions.
                    writer.append(",");
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
        if (needFLine || !append) {
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

    public static void exportMatrix(double[][] matrixValue, String[] colName, String[] rowName, File fileOut) throws IOException {
        CSVWriter w = getCSVWriter(fileOut);
        String[] newColName = new String[colName.length + 1];
        newColName[0] = "";
        System.arraycopy(colName, 0, newColName, 1, colName.length);
        w.writeNext(newColName);
        for (int i = 0; i < rowName.length; i++) {
            String[] line = new String[colName.length + 1];
            line[0] = rowName[i];
            for (int j = 1; j <= colName.length; j++)
                line[j] = String.valueOf(matrixValue[i][j - 1]);
            w.writeNext(line);
        }
        w.close();
    }

    /**
     * Export a simple matrix to be read with python/Julia
     *
     * @param matrixValue tab
     * @param fileOut     .txt file
     * @throws IOException writing file
     */
    public static void exportMatrix(double[][] matrixValue, File fileOut) throws IOException {
        CSVWriter w = new CSVWriter(new FileWriter(fileOut.getName().endsWith(".txt") ? fileOut : new File(fileOut.getParentFile(), fileOut.getName() + ".txt"), false),
                ' ', CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.NO_ESCAPE_CHARACTER, lineEnd);
        for (double[] doubles : matrixValue) {
            String[] line = new String[matrixValue.length];
            for (int j = 0; j < line.length; j++)
                line[j] = String.valueOf(doubles[j]);
            w.writeNext(line);
        }
        w.close();
    }
}
