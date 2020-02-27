package fr.ign.cogit.geoToolsFunctions;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

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
 * @author yo
 *
 */
public class Csv {
	public static boolean needFLine = true;

//	public static void main(String[] args) {
//
//	}

	
	public static void mergeCSVFiles(File rootFolder, File outFile) throws IOException {
		List<File> listCSV = new ArrayList<File>();
		for (File folderToMerge : rootFolder.listFiles()) {
			for (File fileToMerge : folderToMerge.listFiles()) {
				if (fileToMerge.toString().endsWith(".csv")) {
					listCSV.add(fileToMerge);
				}
			}
		}
		mergeCSVFiles(listCSV, outFile);
	}

	public static void mergeCSVFiles(List<File> filesToMerge, File outFile) throws IOException {
		mergeCSVFiles(filesToMerge, outFile, false);
	}

	public static void mergeCSVFiles(List<File> filesToMerge, File outFile, boolean replace) throws IOException {
		if (outFile.exists()) {
			if (replace) {
				Files.delete(outFile.toPath());
			} else {
				return;
			}
		}

		CSVReader defCsv = new CSVReader(new FileReader(filesToMerge.get(0)));
		String[] firstLineDef = defCsv.readNext();
		defCsv.close();
		CSVWriter output = new CSVWriter(new FileWriter(outFile));
		output.writeNext(firstLineDef);
		for (File fileToMerge : filesToMerge) {
			CSVReader ptCsv = new CSVReader(new FileReader(fileToMerge));
			String[] firstLine = ptCsv.readNext();
			if (firstLine.length != firstLineDef.length) {
				System.out.println("mergeCSVFiles() method merges different files");
			}
			for (String[] line : ptCsv) {
				output.writeNext(line);
			}
			ptCsv.close();
		}
		output.close();
	}
	
	/**
	 * put the raw values of a .tif raster file cells to a .csv format
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
		Hashtable<String, Double> cells = new Hashtable<String, Double>();

		for (int i = debI; i < w; i++) {
			for (int j = debJ; j < h; j++) {
				GridCoordinates2D coord = new GridCoordinates2D(i, j);
				double[] temp = coverage.evaluate(coord, vals);
				if (temp[0] > 0.001) {
					cells.put(coord.toString(), temp[0]);
				}
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
	 * generate a .csv from a collection Hashtable<String, Hashtable<String,
	 * Double[]>> Used mainly for the MUP-City particular evaluation analysis
	 * 
	 * @param results
	 * @param file
	 * @param name
	 * @throws IOException
	 */
	public static void generateCsvFileMultTab(Hashtable<String, Hashtable<String, Double[]>> results, String name,
			String fLine, File file) throws IOException {

		File fileName = new File(file + "/" + name + ".csv");
		FileWriter writer = new FileWriter(fileName, true);
		for (String tab : results.keySet()) {
			Hashtable<String, Double[]> intResult = results.get(tab);
			if (needFLine) {
				writer.append("scenario " + tab + "," + fLine + "\n");
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
	 * generate a .csv from a collection Hashtable<String, Hashtable<String,
	 * Double>> Used mainly for the MUP-City analysis objects RasterMergeResult
	 * 
	 * @param results
	 * @param file
	 * @param name
	 * @throws IOException
	 */
	public static void generateCsvFileMultTab(Hashtable<String, Hashtable<String, Double>> results, File file,
			String name) throws IOException {

		File fileName = new File(file + "/" + name + ".csv");
		FileWriter writer = new FileWriter(fileName, true);
		for (String tab : results.keySet()) {
			Hashtable<String, Double> intResult = results.get(tab);
			writer.append("scenario " + tab + "\n");
			for (String nomScenar : intResult.keySet()) {
				writer.append(nomScenar + "," + intResult.get(nomScenar));
				writer.append("\n");
			}
			writer.append("\n");
		}
		writer.close();
	}

	public static void generateLatexMultTab(Hashtable<String, Hashtable<String, Double>> results, File file,
			String name) throws IOException {

		File fileName = new File(file + "/" + name + ".txt");
		FileWriter writer = new FileWriter(fileName, true);
		for (String tab : results.keySet()) {
			Hashtable<String, Double> intResult = results.get(tab);
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
	 * for what is it used?
	 * 
	 * @param cellRepet
	 * @param file
	 * @param name
	 * @throws IOException
	 */
	public static void generateCsvFile(Hashtable<String, Object[]> cellRepet, File file, String name)
			throws IOException {
		Hashtable<String, double[]> result = new Hashtable<String, double[]>();
		for (Object[] ligne : cellRepet.values()) {
			double[] aMettre = new double[ligne.length - 1];
			for (int i = 1; i < ligne.length; i = i + 1) {
				aMettre[i - 1] = (double) ligne[i];
			}
			result.put(String.valueOf(ligne[0]), aMettre);
		}
		generateCsvFileCol(result, file, name);
	}

	// public static void generateCsvFile(Hashtable<String, double[]> cellRepet,
	// File file, String name,
	// String[] premiereColonne) throws IOException {
	// File fileName = new File(file + "/" + name + ".csv");
	// boolean addAfter = true;
	// FileWriter writer = new FileWriter(fileName, addAfter);
	// if (premiereColonne != null) {
	// for (String title : premiereColonne) {
	// writer.append(title + ",");
	// }
	// writer.append("\n");
	// }
	//
	// for (String nomScenar : cellRepet.keySet()) {
	// writer.append(nomScenar + ",");
	// for (double val : cellRepet.get(nomScenar)) {
	// writer.append(val + ",");
	// }
	// writer.append("\n");
	// }
	// writer.close();
	// }

	
	public static void generateCsvFile(Hashtable<String, double[]> cellRepet, File file, String name,
			String[] premiereColonne) throws IOException {
		generateCsvFile(cellRepet, file, name, premiereColonne,true);
	}
	
	/**
	 * 
	 * @param cellRepet
	 * @param file
	 * @param name
	 * @param premiereColonne
	 * @throws IOException
	 */
	public static void generateCsvFile(Hashtable<String, double[]> cellRepet, File file, String name,
			String[] premiereColonne,boolean append) throws IOException {
		String fLine = "";
		if (premiereColonne != null) {
			fLine = premiereColonne[0];
			for (int i = 1; i < premiereColonne.length; i++) {
				fLine = (fLine + "," + premiereColonne[i]);
			}
		}

		List<String> lines = new ArrayList<String>();
		for (String nom : cellRepet.keySet()) {
			String line = nom + ",";
			for (double val : cellRepet.get(nom)) {
				line = line + val+",";
			}
			lines.add(line);
		}
		simpleCSVWriter(lines, fLine, new File(file, name + ".csv"), append);
	}
	
	public static void generateCsvFile(Hashtable<String, String[]> cellRepet, File file, String name, boolean append, String[] premiereColonne)
			throws IOException {
		String fLine = "";
		if (premiereColonne != null) {
			fLine = premiereColonne[0];
			for (int i = 1; i < premiereColonne.length; i++) {
				fLine = (fLine + "," + premiereColonne[i]);
			}
		}

		List<String> lines = new ArrayList<String>();
		for (String nom : cellRepet.keySet()) {
			String line = nom + ",";
			for (String val : cellRepet.get(nom)) {
				line = line + val + ",";
			}
			lines.add(line);
		}
		simpleCSVWriter(lines, fLine, new File(file, name + ".csv"), append);
	}

	public static void generateCsvFileCol(Hashtable<String, double[]> cellRepet, File file, String name)
			throws IOException {
		File fileName = new File(file + "/" + name + ".csv");
		FileWriter writer = new FileWriter(fileName, false);

		// selec the longest tab
		int longestTab = 0;
		for (double[] tab : cellRepet.values()) {
			if (tab.length > longestTab) {
				longestTab = tab.length;
			}
		}
		// put the main names
		for (String nomm : cellRepet.keySet()) {
			writer.append(nomm + ",");
		}
		writer.append("\n");

		for (int i = 0; i <= longestTab - 1; i++) {
			for (String nomm : cellRepet.keySet()) {
				try {
					writer.append(Double.toString(cellRepet.get(nomm)[i]) + ",");
				} catch (ArrayIndexOutOfBoundsException a) {
				}
			}
			writer.append("\n");
		}

		writer.close();
	}

	public static void simpleCSVWriter(List<String> lines, String firstLine, File f, boolean append)
			throws IOException {
		if (!f.getName().endsWith(".csv")) {
			f = new File(f + ".csv");
		}
		f.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(f, append);
		boolean fL = true;
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
