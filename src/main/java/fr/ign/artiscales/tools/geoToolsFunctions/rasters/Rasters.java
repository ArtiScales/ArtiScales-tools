package fr.ign.artiscales.tools.geoToolsFunctions.rasters;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import it.geosolutions.jaiext.JAIExt;
import org.geotools.coverage.grid.GridCoverage2D;
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
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.raster.BandMergeProcess;
import org.geotools.process.vector.VectorToRasterProcess;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class Rasters {

/*    public static void main(String[] args) throws IOException {
        DataStore ds = Collec.getDataStore(new File("/home/mc/Documents/inria/donnees/IGN/batVeme.gpkg"));
        rasterize(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), new Dimension(10000, 10000), ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().getBounds());

        DataStore ds = Collec.getDataStore(new File("/home/mc/Documents/inria/donnees/POI/SIRENE-WorkingPlace.gpkg"));
        String[]       argIn = { "workforceNormalized","amenityCodeNormalized"};
        SimpleFeatureCollection sfc = Collec.convertAttributeToFloat(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), Arrays.asList(argIn));
        String[]       argOut = { "workforceCode","amenityCode"};

        GridCoverage2D r = Rasters.rasterize(sfc,argIn , new Dimension(10000, 10000), sfc.getBounds(), argOut);
        System.out.println(r);

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
     * @return the rasterized collection of {@link org.opengis.feature.simple.SimpleFeature}
     */
    public static GridCoverage2D rasterize(SimpleFeatureCollection features, Object[] attributes, Dimension gridDim, Envelope bounds, String[] covNames) throws IOException {
        JAIExt.initJAIEXT();
        if (attributes.length != covNames.length) {
            System.out.println("rasterize: lenght of arrays of attributes and the covNames must be equals. Return null");
            return null;
        }
        GridCoverage2D ini = VectorToRasterProcess.process(Collec.convertAttributeToFloat(features, (String) attributes[0]), attributes[0], gridDim, bounds, covNames[0], null);

        for (int i = 1; i < attributes.length; i++) {
            BandMergeProcess bm = new BandMergeProcess();
            ini = bm.execute(Arrays.asList(ini, VectorToRasterProcess.process(Collec.convertAttributeToFloat(features, (String) attributes[i]), attributes[i], gridDim, bounds, covNames[i], null)), null, null, null);
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
     * @param envelope File containing the {@link SimpleFeatureCollection} of the mask
     * @param fileOut
     * @throws IOException writer
     */
    public static void crop(File fileToCut, File envelope, File fileOut) throws IOException {
        DataStore envDS = Collec.getDataStore(envelope);
        writeGeotiff(Rasters.importRaster(fileToCut, Geom.unionSFC(envDS.getFeatureSource(envDS.getTypeNames()[0]).getFeatures())), fileOut);
        envDS.dispose();
    }

//	public static void main(String[] args) throws InvalidParameterValueException, ParameterNotFoundException, IOException, TransformException {
//		cut(new File(
//				"/media/mcolomb/Data_2/resultFinal/sens/cellSize/cellSize-Manu-CM17.0-S0.0-GP_915948.0_6677337.0/N4_St_Moy_ahpS_seed_42/N4_St_Moy_ahpS_seed_42-analyse-17.0.tif"),
//				new File("/home/mcolomb/informatique/MUP/explo/emprise/tada/emprise.shp"), new File("/home/mcolomb/tmp/tmp.tif"));
//		cut(new File(
//				"/media/mcolomb/Data_2/resultFinal/sens/cellSize/cellSize-Manu-CM17.0-S0.0-GP_915948.0_6677337.0/N4_St_Moy_ahpS_seed_42/N4_St_Moy_ahpS_seed_42-analyse-51.0.tif"),
//				new File("/home/mcolomb/informatique/MUP/explo/emprise/tada/emprise.shp"), new File("/home/mcolomb/tmp/tmp2.tif"));
//	}

    public static void writeGeotiff(GridCoverage2D coverage, File fileName) {
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
    }
}
