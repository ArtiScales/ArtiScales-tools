package fr.ign.cogit.geoToolsFunctions;

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.TransformException;

import fr.ign.cogit.geoToolsFunctions.vectors.Geom;

public class Rasters {

	public static GridCoverage2D importRaster(File rasterIn) throws IOException {
		ParameterValue<OverviewPolicy> policy = AbstractGridFormat.OVERVIEW_POLICY.createValue();
		policy.setValue(OverviewPolicy.IGNORE);
		ParameterValue<String> gridsize = AbstractGridFormat.SUGGESTED_TILE_SIZE.createValue();
		ParameterValue<Boolean> useJaiRead = AbstractGridFormat.USE_JAI_IMAGEREAD.createValue();
		useJaiRead.setValue(true);
		GeneralParameterValue[] params = new GeneralParameterValue[] { policy, gridsize, useJaiRead };
		GridCoverage2DReader reader = new GeoTiffReader(rasterIn);
		GridCoverage2D coverage = reader.read(params);
		return coverage;
	}

	public static void cut(File fileToCut, File envelope, File fileOut) throws IOException, InvalidParameterValueException, ParameterNotFoundException, TransformException {
		ShapefileDataStore envDS = new ShapefileDataStore(envelope.toURI().toURL());
		Geometry env = Geom.unionSFC(envDS.getFeatureSource().getFeatures());
		writeGeotiff(fileOut, Rasters.importRaster(fileToCut, env));
	}

//	public static void main(String[] args) throws InvalidParameterValueException, ParameterNotFoundException, IOException, TransformException {
//		cut(new File(
//				"/media/mcolomb/Data_2/resultFinal/sens/cellSize/cellSize-Manu-CM17.0-S0.0-GP_915948.0_6677337.0/N4_St_Moy_ahpS_seed_42/N4_St_Moy_ahpS_seed_42-analyse-17.0.tif"),
//				new File("/home/mcolomb/informatique/MUP/explo/emprise/tada/emprise.shp"), new File("/home/mcolomb/tmp/tmp.tif"));
//		cut(new File(
//				"/media/mcolomb/Data_2/resultFinal/sens/cellSize/cellSize-Manu-CM17.0-S0.0-GP_915948.0_6677337.0/N4_St_Moy_ahpS_seed_42/N4_St_Moy_ahpS_seed_42-analyse-51.0.tif"),
//				new File("/home/mcolomb/informatique/MUP/explo/emprise/tada/emprise.shp"), new File("/home/mcolomb/tmp/tmp2.tif"));
//	}
		
	public static void writeGeotiff(File fileName, GridCoverage2D coverage) {
		try {
			GeoTiffWriteParams wp = new GeoTiffWriteParams();
			wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
			wp.setCompressionType("LZW");
			ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
			params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
			GeoTiffWriter writer = new GeoTiffWriter(fileName);
			writer.write(coverage, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static GridCoverage2D importRaster(File f, Geometry mask) throws InvalidParameterValueException, ParameterNotFoundException, IOException, TransformException {
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

	// public static void writeGeotiff(File fileName, float[][] imagePixelData, Envelope2D env) {
	// GridCoverage2D coverage = new GridCoverageFactory().create("OTPAnalyst", imagePixelData, env);
	// writeGeotiff(fileName, coverage);
	// }
	//
	// public static void writeGeotiff(File fileName, GridCoverage2D coverage) {
	// try {
	// //TODO do that withou thema.édata
	// IOImage.saveTiffCoverage(fileName, coverage);
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// try {
	// GeoTiffWriteParams wp = new GeoTiffWriteParams();
	// wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
	// wp.setCompressionType("LZW");
	// ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
	// params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
	// GeoTiffWriter writer = new GeoTiffWriter(fileName);
	// writer.write(coverage, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
	// } catch (Exception e) {
	//
	// e.printStackTrace();
	// }
	//
	// }
}
