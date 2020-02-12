package fr.ign.cogit.GTFunctions;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Schemas {
	public static SimpleFeatureBuilder getBasicSchema(String name) throws NoSuchAuthorityCodeException, FactoryException {

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public static SimpleFeatureBuilder getBasicSchemaMultiPolygon(String name) throws NoSuchAuthorityCodeException, FactoryException {

		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add("the_geom", MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}
	
	public static SimpleFeatureBuilder getMUPAmenitySchema(String name) throws NoSuchAuthorityCodeException, FactoryException {
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		SimpleFeatureTypeBuilder PointSfTypeBuilder = new SimpleFeatureTypeBuilder();
		PointSfTypeBuilder.setCRS(sourceCRS);
		PointSfTypeBuilder.setName(name);
		PointSfTypeBuilder.add("the_geom", Point.class);
		PointSfTypeBuilder.setDefaultGeometry("the_geom");
		PointSfTypeBuilder.add("TYPE", String.class);
		PointSfTypeBuilder.add("LEVEL", Integer.class);

		SimpleFeatureType pointFeatureType = PointSfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(pointFeatureType);
	}
	
	public static SimpleFeatureBuilder getMUPRoadSchema() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("road");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", MultiLineString.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("Speed", Integer.class);
		sfTypeBuilder.add("nature", String.class);
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}
}
