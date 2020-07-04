package fr.ign.cogit.geoToolsFunctions;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.geoToolsFunctions.vectors.Collec;

public class Schemas {
	public static SimpleFeatureBuilder getBasicSchema(String name) throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add(Collec.getDefaultGeomName(), Polygon.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}
	
	public static SimpleFeatureBuilder getBasicSchemaID(String name) throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add(Collec.getDefaultGeomName(), Polygon.class);
		sfTypeBuilder.add("id", Integer.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public static SimpleFeatureBuilder getBasicSchemaMultiPolygon(String name) throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add(Collec.getDefaultGeomName(), MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}
	
	public static SimpleFeatureBuilder getMUPAmenitySchema(String name) throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder PointSfTypeBuilder = new SimpleFeatureTypeBuilder();
		PointSfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		PointSfTypeBuilder.setName(name);
		PointSfTypeBuilder.add(Collec.getDefaultGeomName(), Point.class);
		PointSfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		PointSfTypeBuilder.add("TYPE", String.class);
		PointSfTypeBuilder.add("LEVEL", Integer.class);
		SimpleFeatureType pointFeatureType = PointSfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(pointFeatureType);
	}
	
	public static SimpleFeatureBuilder getMUPRoadSchema() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.setName("road");
		sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		sfTypeBuilder.add(Collec.getDefaultGeomName(), MultiLineString.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		sfTypeBuilder.add("SPEED", Integer.class);
		sfTypeBuilder.add("NATURE", String.class);
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public static SimpleFeatureBuilder getASCommunitySchema() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		sfTypeBuilder.add(Collec.getDefaultGeomName(), MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		sfTypeBuilder.add("DEPCOM", String.class);
		sfTypeBuilder.add("NOM_COM", String.class);
		sfTypeBuilder.add("typo", String.class);
		sfTypeBuilder.add("surface", String.class);
		sfTypeBuilder.add("scot", String.class);
		sfTypeBuilder.add("log-icone", String.class);
		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}
	
	public static SimpleFeatureBuilder getSFBSchemaWithMultiPolygon(SimpleFeatureType schema) {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		String geomName = schema.getGeometryDescriptor().getLocalName();
		for (AttributeDescriptor attr : schema.getAttributeDescriptors()) {
			if (attr.getLocalName().equals(geomName))
				continue;
			sfTypeBuilder.add(attr);
		}
		sfTypeBuilder.setName(schema.getName());
		sfTypeBuilder.setCRS(schema.getCoordinateReferenceSystem());
		sfTypeBuilder.add(geomName, MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry(geomName);
		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}
	
	public static SimpleFeatureBuilder setSFBSchemaWithMultiPolygon(SimpleFeature feat) {
		SimpleFeatureBuilder builder = getSFBSchemaWithMultiPolygon(feat.getFeatureType());
		for (AttributeDescriptor attr : feat.getFeatureType().getAttributeDescriptors())
			builder.set(attr.getName(), feat.getAttribute(attr.getName()));
		return builder;
	}
}
