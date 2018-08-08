package fr.ign.cogit.GTFunctions;

import java.io.File;
import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.OverviewPolicy;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

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
}
