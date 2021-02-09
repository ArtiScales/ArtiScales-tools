package fr.ign.artiscales.tools.geoToolsFunctions.rasters;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import it.geosolutions.jaiext.JAIExt;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.coverage.processing.operation.AddConst;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.raster.BandMergeProcess;
import org.geotools.process.vector.VectorToRasterProcess;
import org.locationtech.jts.geom.Geometry;
import org.opengis.coverage.grid.GridCoordinates;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class Rasters {

/*    public static void main(String[] args) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(new File("/home/mc/Nextcloud/boulot/inria/ICIproject/donnees/IGN/batVeme.gpkg"));
        GridCoverage2D r = rasterize(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(),"PREC_PLANI", getDimentionValuesForSquaredRasters(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), 1), ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().getBounds(),
                "PREC_PLANI");

*//*
        DataStore ds = CollecMgmt.getDataStore(new File("/home/mc/Documents/inria/donnees/POI/SIRENE-WorkingPlace.gpkg"));
        String[]       argIn = { "workforceNormalized","amenityCodeNormalized"};
        SimpleFeatureCollection sfc = CollecMgmt.convertAttributeToFloat(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), Arrays.asList(argIn));
        String[]       argOut = { "workforceCode","amenityCode"};

        GridCoverage2D r = Rasters.rasterize(sfc,argIn , new Dimension(10000, 10000), sfc.getBounds(), argOut);
        System.out.println(r);
*//*

        writeGeotiff(r, new File("/tmp/rast2"));

    }*/

    /**
     * Rasterize a single field of the given simple feature collection
     *
     * @param features collection of {@link org.opengis.feature.simple.SimpleFeature}
     * @param gridDim  resolution of the grid
     * @return the rasterized collection of {@link org.opengis.feature.simple.SimpleFeature}
     */
    public static GridCoverage2D rasterize(SimpleFeatureCollection features, String attribute, Dimension gridDim, String attrOut) {
        return VectorToRasterProcess.process(features, attribute, gridDim, features.getBounds(), attrOut, null);
    }

    /**
     * Rasterize a single field of the given simple feature collection
     *
     * @param features  collection of {@link org.opengis.feature.simple.SimpleFeature}
     * @param attribute attribute name to put its value in the raster (must be a String)
     * @param gridDim   resolution of the grid
     * @param bounds    bounds of the grid
     * @param covName   name of the converted field
     * @return the rasterized collection of {@link org.opengis.feature.simple.SimpleFeature}
     */
    public static GridCoverage2D rasterize(SimpleFeatureCollection features, Object attribute, Dimension gridDim, Envelope bounds, String covName) {
        return VectorToRasterProcess.process(features, attribute, gridDim, bounds, covName, null);
    }

    /**
     * Rasterize a single field of the given simple feature collection. No attribute value. Not tested
     *
     * @param features collection of {@link org.opengis.feature.simple.SimpleFeature}
     * @param gridDim  resolution of the grid
     * @param bounds   bounds of the grid
     * @return the rasterized collection of {@link org.opengis.feature.simple.SimpleFeature}
     */
    public static GridCoverage2D rasterize(SimpleFeatureCollection features, Dimension gridDim, Envelope bounds) {
        //get an attribute which class is int, float, or double
        System.out.println("rasterize(SimpleFeatureCollection features, Dimension gridDim, Envelope bounds) : not tested");
        SimpleFeature sf = features.features().next();
        String attr = "";
        for (int i = 0; i < sf.getAttributeCount(); i++) {
            if (sf.getAttribute(i) instanceof Float || sf.getAttribute(i) instanceof Double) {
                attr = sf.getFeatureType().getType(i).getName().toString();
                break;
            }
            if (i == sf.getAttributeCount() - 1)
                System.out.println("rasterize : no good value");
        }

        GridCoverage2D unNormalized = VectorToRasterProcess.process(features, attr, gridDim, bounds, "null", null);
        AddConst cst = new AddConst();
        writeGeotiff(unNormalized, new File("/tmp/before"));
        ParameterValueGroup param = CoverageProcessor.getInstance().getOperation("AddConst").getParameters();
        param.parameter("source").setValue(unNormalized);
        param.parameter("constants").setValue("1.0");

        System.out.println("param" + param);
        GridCoverage2D normalized = (GridCoverage2D) cst.doOperation(param, null);
        writeGeotiff(normalized, new File("/tmp/normalized"));

//        param.parameter("sources").setValue(coverages);

        return normalized;
    }

    /**
     * Rasterize a multiple fields of the given simple feature collection
     *
     * @param features   collection of {@link org.opengis.feature.simple.SimpleFeature}
     * @param attributes multiple attribute names to put their value in the raster
     * @param gridDim    resolution of the grid
     * @param bounds     bounds of the grid
     * @param covNames   name of the converted fields. Must be the same length than attributes
     * @param separateRasters if true, export each band of a raster in a separate file. If false, merge every attribute in a band
     * @param folderOut Folder to export (can be <b>null</b> if separateRasters is <b>true</b>
     * @return the rasterized collection of {@link org.opengis.feature.simple.SimpleFeature}
     */
    public static GridCoverage2D rasterize(SimpleFeatureCollection features, Object[] attributes, Dimension gridDim, Envelope bounds, String[] covNames, boolean separateRasters, File folderOut) throws IOException {
        JAIExt.initJAIEXT();
        if (attributes.length != covNames.length) {
            System.out.println("rasterize: lenght of arrays of attributes and the covNames must be equals. Return null");
            return null;
        }
        GridCoverage2D ini = VectorToRasterProcess.process(CollecMgmt.convertAttributeToFloat(features, (String) attributes[0]), attributes[0], gridDim, bounds, covNames[0], null);
        if (!separateRasters)
            for (int i = 1; i < attributes.length; i++) {
                BandMergeProcess bm = new BandMergeProcess();
                ini = bm.execute(Arrays.asList(ini, VectorToRasterProcess.process(CollecMgmt.convertAttributeToFloat(features, (String) attributes[i]), attributes[i], gridDim, bounds, covNames[i], null)), null, null, null);
            }
        else {
            writeGeotiff(ini, new File(folderOut, attributes[0]+".tif"));
            for (int i = 1; i < attributes.length; i++)
                writeGeotiff(VectorToRasterProcess.process(CollecMgmt.convertAttributeToFloat(features, (String) attributes[i]), attributes[i], gridDim, bounds, covNames[i], null), new File(folderOut, attributes[i]+".tif"));
        }
        return ini;
    }

    /**
     * Transform a raster file from (?) format intersecting a mask to a {@link GridCoverage2D}
     *
     * @param f    input {@link File}
     * @param mask mask under where only raster will be imported
     * @return the raster with values under the mask
     * @throws IOException geotiffReader
     */
    public static GridCoverage2D importRaster(File f, Geometry mask) throws IOException {
        CoverageProcessor processor = CoverageProcessor.getInstance();
        final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
        GridCoverage2D gridCoverage = importRaster(f);
        ReferencedEnvelope envelope = JTS.toEnvelope(mask);
        if (!gridCoverage.getEnvelope2D().intersects(envelope))
            return null;
        param.parameter("Source").setValue(gridCoverage);
        param.parameter("Envelope").setValue(envelope);
        return (GridCoverage2D) processor.doOperation(param);
    }

    /**
     * Transform a raster file from (?) format to a {@link GridCoverage2D}
     *
     * @param rasterIn file to convert
     * @return the corresponding #GridCoverage2D
     * @throws IOException geotiff reader
     */
    public static GridCoverage2D importRaster(File rasterIn) throws IOException {
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);
        ParameterValue<String> gridSize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(true);
        GeneralParameterValue[] params = new GeneralParameterValue[]{policy, gridSize, useJaiRead};
        GridCoverage2DReader reader = new GeoTiffReader(rasterIn);
        return reader.read(params);
    }

    /**
     * Untested (yet) maybe other built in methods exists
     *
     * @param fileToCut geotiff to crop
     * @param envelope  File containing the {@link SimpleFeatureCollection} of the mask
     * @param fileOut
     * @throws IOException writer
     */
    public static void crop(File fileToCut, File envelope, File fileOut) throws IOException {
        DataStore envDS = CollecMgmt.getDataStore(envelope);
        writeGeotiff(Rasters.importRaster(fileToCut, Geom.unionSFC(envDS.getFeatureSource(envDS.getTypeNames()[0]).getFeatures())), fileOut);
        envDS.dispose();
    }

    /**
     * Get the dimention for a grid of a given resolution
     *
     * @param bbox
     * @param sideSize
     * @return
     */
    public static Dimension getDimentionValuesForSquaredRasters(SimpleFeatureCollection bbox, float sideSize) {
        return getDimentionValuesForSquaredRasters(bbox.getBounds(), sideSize);
    }

    /**
     * Get the dimention for a grid of a given resolution
     *
     * @param env      Input BoundingBox
     * @param sideSize
     * @return
     */
    public static Dimension getDimentionValuesForSquaredRasters(ReferencedEnvelope env, float sideSize) {
        int nbWidth = Math.round((float) env.getWidth() / sideSize);
        int nbHeight = Math.round((float) env.getHeight() / sideSize);
        return new Dimension(nbWidth, nbHeight);
    }

    public static File writeGeotiff(GridCoverage2D coverage, File fileName) {
        try {
            GeoTiffWriteParams wp = new GeoTiffWriteParams();
            wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
            wp.setCompressionType("LZW");
            ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
            params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
            GeoTiffWriter writer = new GeoTiffWriter(fileName);
            writer.write(coverage, params.values().toArray(new GeneralParameterValue[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fileName;
    }

    public static File writeGeotiff(HashMap<DirectPosition2D, Float> table, int ech, File fileOut, File exempleRaster)
            throws IOException {
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);
        // this will basically read 4 tiles worth of data at once from the
        // disk...
        ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
        // Setting read type: use JAI ImageRead (true) or ImageReaders read
        // methods (false)
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(false);
        GeneralParameterValue[] params = new GeneralParameterValue[]{policy, gridsize, useJaiRead};

        // set matrice

        GeoTiffReader readerSet = new GeoTiffReader(exempleRaster);
        GridCoverage2D coverageSet = readerSet.read(params);
        Envelope2D env = coverageSet.getEnvelope2D();

        float[][] imagePixelData = new float[(int) Math.floor(env.getWidth() / ech)][(int) Math
                .floor(env.getHeight() / ech)];

        int longueur = imagePixelData.length;
        int largeur = imagePixelData[0].length;

        for (int i = 0; i < longueur; ++i)
            for (int j = 0; j < largeur; ++j)
                try {
                    imagePixelData[i][j] = table.get(new DirectPosition2D(i, j));
                } catch (NullPointerException ignored) {
                }

        // todo clean this mess
        float[][] imgpix2 = new float[imagePixelData[0].length][imagePixelData.length];
        float[][] imgpix3 = new float[imagePixelData[0].length][imagePixelData.length];
        for (int i = 0; i < imgpix2.length; ++i) {
            for (int j = 0; j < imgpix2[0].length; ++j) {
                imgpix2[i][j] = imagePixelData[imgpix2[0].length - 1 - j][i];
            }
        }
        for (int i = 0; i < imgpix3.length; ++i) {
            for (int j = 0; j < imgpix3[0].length; ++j) {
                imgpix3[i][j] = imgpix2[imgpix3.length - 1 - i][imgpix3[0].length - 1 - j];
            }
        }
        return writeGeotiff(fileOut, imgpix3, env);
    }

    public static File writeGeotiff(File fileName, float[][] imagePixelData, Envelope2D env) {
        GridCoverage2D coverage = new GridCoverageFactory().create("OTPAnalyst", imagePixelData, env);
        return writeGeotiff(coverage, fileName);
    }

    /**
     *
     * @param dim bound of the wanted raster
     * @param cellResolution resolution of the wanted raster
     * @param rasterExample similar raster todo get rid of that somehow
     * @param outFolder output folder
     * @return the file on which the raster has been written
     * @throws IOException
     */
    public static File createRasterWithID(Dimension dim, int cellResolution, File rasterExample, File outFolder) throws IOException {
        HashMap<DirectPosition2D, Float> tab = new HashMap<>();
        float val = 0;
        for (int i = 0; i < ((int) dim.getWidth()); i++)
            for (int j = 0; j < ((int) dim.getHeight()); j++)
                tab.put(new DirectPosition2D(i, j), val++);
        return Rasters.writeGeotiff(tab, cellResolution, new File(outFolder, "rasterID.tif"), rasterExample);
    }
    /**
     * Put the not nulls values of a .tif raster file cells to a .csv format.
     *
     * @param fileToConvert
     * @throws IOException
     */
    public static File convertRasterPositivesValuesToCsv(File fileToConvert) throws IOException {
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);
        ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(false);
        GeneralParameterValue[] params = new GeneralParameterValue[]{policy, gridsize, useJaiRead};

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
        HashMap<String, Double> cells = new HashMap<>();
        for (int i = debI; i < w; i++)
            for (int j = debJ; j < h; j++) {
                GridCoordinates2D coord = new GridCoordinates2D(i, j);
                double[] temp = coverage.evaluate(coord, vals);
                if (temp[0] > 0.001)
                    cells.put(coord.toString(), temp[0]);
            }

        // RasterAnalyse.generateCsvFileCol(cells,new File (rastFile.getParent()),);
        File fileName = new File(fileToConvert + "-tocsv.csv");
        FileWriter writer = new FileWriter(fileName, false);
        writer.append("eval");
        writer.append("\n");
        for (String nomm : cells.keySet()) {
            double tableau = cells.get(nomm);
            for (int i = 0; i < tableau; i++)
                writer.append(Double.toString(tableau)).append("\n");
        }
        writer.close();
        return fileName;
    }

    public static File convertRasterAndPositionValuesToCsv(File fileToConvert, File rasterId, String posValueName) throws IOException {
        return convertRasterAndPositionValuesToCsv(fileToConvert, rasterId, posValueName, 0);
    }
            /**
             * Put the values of a .tif raster file cells to a .csv format corresponding to a raster with id.
             * todo make it work for multiple bands and attribute columns
             * @param fileToConvert raster file containing the field to convert
             * @param rasterId raster file containing the coordinates
             * @param posValueName attribute name which will in the .csv header
             * @throws IOException
             */
    public static File convertRasterAndPositionValuesToCsv(File fileToConvert, File rasterId, String posValueName, int bandNumber) throws IOException {
        ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
        policy.setValue(OverviewPolicy.IGNORE);
        ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
        ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
        useJaiRead.setValue(false);
        GeneralParameterValue[] params = new GeneralParameterValue[]{policy, gridsize, useJaiRead};

        //import file to convert
        GridCoverage2DReader reader = new GeoTiffReader(fileToConvert);
        GridCoverage2D file2ConvCoverage = reader.read(params);
        GridEnvelope dimensionsCoverage = reader.getOriginalGridRange();
        GridCoordinates maxDimensionsCoverage = dimensionsCoverage.getHigh();

        //import file with coordinates
        GridCoverage2DReader readerCoordinate = new GeoTiffReader(rasterId);
        GridCoverage2D coordCoverage = readerCoordinate.read(params);
        GridEnvelope dimensionsCoord = readerCoordinate.getOriginalGridRange();
        GridCoordinates maxDimensionsCoodr = dimensionsCoord.getHigh();

        //integrity check if coord and file to convert are the same
        if (maxDimensionsCoverage.getCoordinateValue(0) != maxDimensionsCoodr.getCoordinateValue(0) &&
                maxDimensionsCoverage.getCoordinateValue(1) != maxDimensionsCoodr.getCoordinateValue(1))
            System.out.println("convertRasterAndPositionValuesToCsv() : coordinate and value raster doesn't corresponds");

        int w = maxDimensionsCoverage.getCoordinateValue(0);
        int h = maxDimensionsCoverage.getCoordinateValue(1);
        double[] vals = new double[reader.getGridCoverageCount()];

        HashMap<Double, Double> cells = new HashMap<>();
//      HashMap<Integer, double[]> cells = new HashMap<>();

        for (int i = 0; i < w; i++)
            for (int j = 0; j < h; j++) {
                GridCoordinates2D coord = new GridCoordinates2D(i, j);
                cells.put(coordCoverage.evaluate(coord,vals)[0], file2ConvCoverage.evaluate(coord, vals)[bandNumber]);
//                cells.put(Math.round((float)coordCoverage.evaluate(coord,vals)[0]), file2ConvCoverage.evaluate(coord, vals));
            }

        // RasterAnalyse.generateCsvFileCol(cells,new File (rastFile.getParent()),);
        File fileName = new File(fileToConvert + "-tocsv.csv");
        FileWriter writer = new FileWriter(fileName, false);

        //set header
        writer.append("coord,").append(posValueName);
//        writer.append("coord,").append(posValueName[0]);
//        for (int i = 1 ; i < posValueName.length ; i++)
//            writer.append(",").append(posValueName[i]);
        writer.append("\n");

        //set values
        for (Double code : cells.keySet()) {
//        for (int code : cells.keySet()) {
//            writer.append(String.valueOf(code)).append(",");
            writer.append(code.toString()).append(",").append(cells.get(code).toString()).append("\n");

//            double[] tableau = cells.get(code);
//            for (int i = 0; i < tableau.length; i++) {
//                if (tableau[i] != 0.0)
//                    System.out.println(tableau[i]);
//                writer.append(String.valueOf(tableau[i])).append(",");
//            }
//            writer.append("\n");
        }
        writer.close();
        return fileName;
    }
}
