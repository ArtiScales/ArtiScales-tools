package fr.ign.cogit.geoToolsFunctions;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

/**
 * Class that contains multiple methods to manipulate a .csv file
 * 
 * @author Maxime Colomb
 *
 */
public class Csv {
	
	public static boolean needFLine = true;

//	public static void main(String[] args) throws IOException {
//		calculateColumnsBasicStat(
//				new File("/home/ubuntu/workspace/ParcelManager/src/main/resources/DensificationStudy/out/densificationStudyResult.csv"), 2, true);
//	}
	/**
	 * Add some basic statistic for a predeterminde column of a csv file. Add extra lines to the end of the .csv.
	 * 
	 * @param csvFile
	 *            path to the .csv {@link File}
	 * @param nbCol
	 *            number of the column to calulate stats (begins at 0)
	 * @param isFirstCol
	 *            true if there's a header and it will be ignored
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
				ds.addValue(Double.valueOf(line[nbCol]));
			} catch (NumberFormatException e) {
			}
		}
		r.close();
		CSVWriter w = new CSVWriter(new FileWriter(csvFile));
		if (isFirstCol)
			w.writeNext(fCol);
		w.writeAll(lines);

		String[] line = new String[lines.get(0).length];
		line[0] = "mean";
		line[nbCol] =String.valueOf(ds.getMean());
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
		List<File> result = new ArrayList<File>();
		for (File f : folder.listFiles()) {
			if (f.isDirectory())
				result.addAll(getCSVFiles(f));
			else if (f.getName().endsWith(".csv"))
				result.add(f);
		}
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
	 * Put the raw values of a .tif raster file cells to a .csv format.
	 * 
	 * @param FileToConvert
	 * @throws IOException
	 */
	public static void convertRasterToCsv(File FileToConvert) throws IOException {
		File fileToConvert = new File("/home/mcolomb/informatique/MUP/explo/enveloppes/evalE-moy-NU.tif");
		ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
		policy.setValue(OverviewPolicy.IGNORE);
		ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
		ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
		useJaiRead.setValue(false);
		GeneralParameterValue[] params = new GeneralParameterValue[] { policy, gridsize, useJaiRead };

		GridCoverage2DReader reader = new GeoTiffReader(fileToConvert);
		GridCoverage2D coverage = reader.read(params);
		GridEnvelope dimensions = reader.getOriginalGridRange();
		GridCoordinates maxDimensions = dimensions.getHigh();

		int w = maxDimensions.getCoordinateValue(0) + 1;
		int h = maxDimensions.getCoordinateValue(1) + 1;
		int numBands = reader.getGridCoverageCount();
		double[] vals = new double[numBands];
		// beginning of the all cells loop
		int debI = 0;
		int debJ = 0;
		HashMap<String, Double> cells = new HashMap<String, Double>();
		for (int i = debI; i < w; i++) {
			for (int j = debJ; j < h; j++) {
				GridCoordinates2D coord = new GridCoordinates2D(i, j);
				double[] temp = coverage.evaluate(coord, vals);
				if (temp[0] > 0.001)
					cells.put(coord.toString(), temp[0]);
			}
		}

		// RasterAnalyse.generateCsvFileCol(cells,new File (rastFile.getParent()),);
		File fileName = new File(fileToConvert + "-tocsv.csv");
		FileWriter writer = new FileWriter(fileName, false);
		writer.append("eval");
		writer.append("\n");
		for (String nomm : cells.keySet()) {
			double tableau = cells.get(nomm);
			for (int i = 0; i < tableau; i++) {
				String in = Double.toString(tableau);
				writer.append(in + "\n");
			}
		}
		writer.close();
	}

	/**
	 * Generates a .csv file out of a {@link HashMap}. Used mainly for the MUP-City particular evaluation analysis
	 * 
	 * @param results
	 * @param folderOut
	 * @param name
	 * @throws IOException
	 */
	public static void generateCsvFileMultTab(HashMap<String, HashMap<String, Double[]>> results, String name,
			String firstLine, File folderOut) throws IOException {
		File fileName = new File(folderOut + "/" + name + ".csv");
		FileWriter writer = new FileWriter(fileName, true);
		for (String tab : results.keySet()) {
			HashMap<String, Double[]> intResult = results.get(tab);
			if (needFLine) {
				writer.append("scenario " + tab + "," + firstLine + "\n");
				needFLine = false;
			}
			for (String nomScenar : intResult.keySet()) {
				writer.append(nomScenar + "," + intResult.get(nomScenar)[0] + "," + intResult.get(nomScenar)[1]);
				writer.append("\n");
			}
			writer.append("\n");
		}
		writer.close();
	}

	/**
	 * Generates a .csv file out of a {@link HashMap}. Used mainly for the MUP-City analysis objects RasterMergeResult
	 * 
	 * @param results
	 * @param folderOut
	 * @param name
	 * @throws IOException
	 */
	public static void generateCsvFileMultTab(HashMap<String, HashMap<String, Double>> results, File folderOut,
			String name) throws IOException {
		File fileName = new File(folderOut + "/" + name + ".csv");
		FileWriter writer = new FileWriter(fileName, true);
		for (String tab : results.keySet()) {
			HashMap<String, Double> intResult = results.get(tab);
			writer.append("scenario " + tab + "\n");
			for (String nomScenar : intResult.keySet()) {
				writer.append(nomScenar + "," + intResult.get(nomScenar));
				writer.append("\n");
			}
			writer.append("\n");
		}
		writer.close();
	}

	/**
	 * This method is supposed to generate latex tabs but not real sure that works @TODO to test
	 * 
	 * @param results
	 * @param folderOut
	 *            Folder where the .csv file is created
	 * @param name
	 *            Name of the .csv file
	 * @throws IOException
	 */
	public static void generateLatexMultTab(HashMap<String, HashMap<String, Double>> results, File folderOut, String name) throws IOException {
		File fileName = new File(folderOut + "/" + name + ".txt");
		FileWriter writer = new FileWriter(fileName, true);
		for (String tab : results.keySet()) {
			HashMap<String, Double> intResult = results.get(tab);
			writer.append("scenario " + tab + "\n");
			for (String nomScenar : intResult.keySet()) {
				writer.append(nomScenar + "&" + intResult.get(nomScenar));
				writer.append("\n");
			}
			writer.append("\n");
		}
		writer.close();
	}

	/**
	 * Generate a .csv file out of a hashtable. Each key is the first entry of a new line. By default, if an already existing .csv file exists, the new data are append to it
	 * 
	 * @param cellRepet
	 *            Values are a double table (they are then converted in Object[])
	 * @param folderOut
	 *            Folder where the .csv file is created
	 * @param name
	 *            Name of the .csv file
	 * @param firstCol
	 *            Header of the .csv file (can be null)
	 * @throws IOException
	 */
	public static void generateCsvFile(HashMap<String, double[]> cellRepet, File folderOut, String name, String[] firstCol)	throws IOException {
		generateCsvFile(cellRepet, name, folderOut, firstCol, true);
	}
	
	/**
	 * generate a .csv file out of a hashtable. Each key is the first entry of a new line
	 * 
	 * @param data
	 *            Values are a double table (they are then converted in Object[])
	 * @param name
	 *            name of the .csv file
	 * @param folderOut
	 *            folder where the .csv file is created
	 * @param firstCol
	 *            header of the .csv file (can be null)
	 * @param append
	 *            in the case an already existing .csv file exists: if true, the new data are append to it. If false, the new table overwritte the old one.
	 * @throws IOException
	 */
	public static void generateCsvFile(HashMap<String, double[]> data, String name, File folderOut, String[] firstCol, boolean append)
			throws IOException {
		HashMap<String, Object[]> result = new HashMap<String, Object[]>();
		for (String key : data.keySet()) {
			double[] line = data.get(key);
			Object[] toPut = new Object[line.length];
			for (int i = 0; i < line.length; i++)
				toPut[i] = line[i];
			result.put(key, toPut);
		}
		generateCsvFile(result, folderOut, name, firstCol, append);
	}

	/**
	 * Generate a .csv file out of a hashtable. Each key is the first entry of a new line
	 * 
	 * @param data
	 *            Values are a String table (they are then converted in Object[])
	 * @param folderOut
	 *            Folder where the .csv file is created
	 * @param name
	 *            Name of the .csv file
	 * @param append
	 *            In the case an already existing .csv file exists: if true, the new data are append to it. If false, the new table overwritte the old one.
	 * @param firstCol
	 *            Header of the .csv file (can be null)
	 * @throws IOException
	 */
	public static void generateCsvFile(HashMap<String, String[]> data, File folderOut, String name, boolean append, String[] firstCol)
			throws IOException {
		HashMap<String, Object[]> result = new HashMap<String, Object[]>();
		for (String key : data.keySet()) {
			String[] line = data.get(key);
			Object[] toPut = new Object[line.length];
			for (int i = 0; i < line.length; i++)
				toPut[i] = line[i];
			result.put(key, toPut);
		}
		generateCsvFile(result, folderOut, name, firstCol, append);
	}

	/**
	 * Generate a .csv file out of a hashtable. Each key is the first entry of a new line. By default, if an already existing .csv file exists, the new data are append to it
	 * 
	 * @param cellRepet
	 *            Values are an Object table
	 * @param name
	 *            Name of the .csv file
	 * @param folderOut
	 *            Folder where the .csv file is created
	 * @param firstCol
	 *            Header of the .csv file (can be null)
	 * @throws IOException
	 */
	public static void generateCsvFile(HashMap<String, Object[]> cellRepet, String name, File folderOut, String[] firstCol) throws IOException {
		generateCsvFile(cellRepet, folderOut, name, firstCol, true);
	}

	/**
	 * Generate a .csv file out of a hashtable. Each key is the first entry of a new line.
	 * 
	 * @param cellRepet
	 *            Values are an Object table
	 * @param folderOut
	 *            Folder where the .csv file is created
	 * @param name
	 *            Name of the .csv file
	 * @param firstCol
	 *            Header of the .csv file (can be null)
	 * @param append
	 *            In the case an already existing .csv file exists: if true, the new data are append to it. If false, the new table overwritte the old one.
	 * @throws IOException
	 */
	public static void generateCsvFile(HashMap<String, Object[]> cellRepet, File folderOut, String name, String[] firstCol, boolean append)
			throws IOException {
		String fLine = "";
		if (firstCol != null) {
			fLine = firstCol[0];
			for (int i = 1; i < firstCol.length; i++)
				fLine = (fLine + "," + firstCol[i]);
		}
		List<String> lines = new ArrayList<String>();
		for (String nom : cellRepet.keySet()) {
			String line = nom + ",";
			for (Object val : cellRepet.get(nom))
				line = line + val + ",";
			lines.add(line);
		}
		simpleCSVWriter(lines, fLine, new File(folderOut, name + ".csv"), append);
	}
	
	/**
	 * Generate a .csv file out of a hashtable. Data are dispalyed in colomn and each key is placed in the header
	 * 
	 * @param cellRepet
	 *            Values are a double table (they are then converted in Object[])
	 * @param name
	 *            name of the .csv file
	 * @param folderOut
	 *            folder where the .csv file is created
	 * @throws IOException
	 */
	public static void generateCsvFileCol(HashMap<String, double[]> cellRepet, String name, File folderOut) throws IOException {
		HashMap<String, Object[]> result = new HashMap<String, Object[]>();
		for (double[] ligne : cellRepet.values()) {
			Object[] aMettre = new Object[ligne.length - 1];
			for (int i = 1; i < ligne.length; i = i + 1)
				aMettre[i - 1] = (double) ligne[i];
			result.put(String.valueOf(ligne[0]), aMettre);
		}
		generateCsvFileCol(result, folderOut, name);
	}

	/**
	 * Generate a .csv file out of a hashtable. Data are dispalyed in colomn and each key is placed in the header
	 * 
	 * @param cellRepet
	 *            Values are an Object table
	 * @param folderOut
	 *            Folder where the .csv file is created
	 * @param name
	 *            Name of the .csv file * @throws IOException
	 */
	public static void generateCsvFileCol(HashMap<String, Object[]> cellRepet, File folderOut, String name) throws IOException {
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
			writer.append(nomm + ",");
		writer.append("\n");
		for (int i = 0; i <= longestTab - 1; i++) {
			for (String nomm : cellRepet.keySet()) {
				try {
					writer.append(cellRepet.get(nomm)[i] + ",");
				} catch (ArrayIndexOutOfBoundsException a) {
					// normal that it gets Arrays exceptions. 
				}
			}
			writer.append("\n");
		}
		writer.close();
	}

	public static void simpleCSVWriter(List<String> lines, String firstLine, File f, boolean append) throws IOException {
		if (!f.getName().endsWith(".csv")) 
			f = new File(f + ".csv");
		f.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(f, append);
		boolean fL = needFLine;
		if (fL) {
			writer.append(firstLine);
			writer.append("\n");
			fL = false;
		}
		for (String l : lines) {
			writer.append(l);
			writer.append("\n");
		}
		writer.close();
	}

	public static void simpleCSVWriter(List<String> lines, File f, boolean append) throws IOException {
		simpleCSVWriter(lines, "", f, append);
	}
}
