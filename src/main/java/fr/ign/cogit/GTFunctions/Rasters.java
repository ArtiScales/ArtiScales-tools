package fr.ign.cogit.GTFunctions;

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.coverage.processing.CoverageProcessor;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

import com.vividsolutions.jts.geom.Envelope;

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

	public static File cut(File fileToCut, File Envelope) throws IOException {
		ShapefileDataStore envDS = new ShapefileDataStore(Envelope.toURI().toURL());
		ReferencedEnvelope env = envDS.getFeatureSource().getFeatures().getBounds();

		GridCoverage2D rast = Rasters.importRaster(fileToCut);

        GridCoverage2D finalCoverage = cropCoverage(rast, env);
		File maskFile = new File("");
		return null;
	}
	
	  private static GridCoverage2D cropCoverage(GridCoverage2D gridCoverage, Envelope envelope) {
	        CoverageProcessor processor = CoverageProcessor.getInstance();

	        // An example of manually creating the operation and parameters we want
	        final ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
	        param.parameter("Source").setValue(gridCoverage);
	        param.parameter("Envelope").setValue(envelope);

	        return (GridCoverage2D) processor.doOperation(param);
	    }
	
//	public static void writeGeotiff(File fileName, float[][] imagePixelData, Envelope2D env) {
//		GridCoverage2D coverage = new GridCoverageFactory().create("OTPAnalyst", imagePixelData, env);
//		writeGeotiff(fileName, coverage);
//	}
//
//	public static void writeGeotiff(File fileName, GridCoverage2D coverage) {
//		try {
//			//TODO do that withou thema.édata
//			IOImage.saveTiffCoverage(fileName, coverage);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		try {
//			GeoTiffWriteParams wp = new GeoTiffWriteParams();
//			wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
//			wp.setCompressionType("LZW");
//			ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
//			params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
//			GeoTiffWriter writer = new GeoTiffWriter(fileName);
//			writer.write(coverage, (GeneralParameterValue[]) params.values().toArray(new GeneralParameterValue[1]));
//		} catch (Exception e) {
//
//			e.printStackTrace();
//		}
//
//	}

}
