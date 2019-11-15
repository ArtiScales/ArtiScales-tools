package fr.ign.cogit;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

public class Schema {
	public static SimpleFeatureBuilder getBasicSFB() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilderSimple = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");

		sfTypeBuilderSimple.setName("basicSFB");
		sfTypeBuilderSimple.setCRS(sourceCRS);
		sfTypeBuilderSimple.add("the_geom", Polygon.class);
		sfTypeBuilderSimple.setDefaultGeometry("the_geom");

		return new SimpleFeatureBuilder(sfTypeBuilderSimple.buildFeatureType());
	}

	public static SimpleFeatureBuilder setSFBParDefaut(SimpleFeature feat, SimpleFeatureType schema, String geometryOutputName) {
		SimpleFeatureBuilder finalParcelBuilder = new SimpleFeatureBuilder(schema);
		finalParcelBuilder.set(geometryOutputName, (Geometry) feat.getDefaultGeometry());
		finalParcelBuilder.set("CODE", "unknow");
		finalParcelBuilder.set("CODE_DEP", "unknow");
		finalParcelBuilder.set("CODE_COM", "unknow");
		finalParcelBuilder.set("COM_ABS", "unknow");
		finalParcelBuilder.set("SECTION", "unknow");
		finalParcelBuilder.set("NUMERO", "unknow");
		finalParcelBuilder.set("INSEE", "unknow");
		finalParcelBuilder.set("eval", "0");
		finalParcelBuilder.set("DoWeSimul", false);
		finalParcelBuilder.set("IsBuild", false);
		finalParcelBuilder.set("U", false);
		finalParcelBuilder.set("AU", false);
		finalParcelBuilder.set("NC", false);

		return finalParcelBuilder;
	}
}
