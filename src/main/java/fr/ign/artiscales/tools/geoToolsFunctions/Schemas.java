package fr.ign.artiscales.tools.geoToolsFunctions;

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

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;

public class Schemas {

	private static String epsg = "EPSG:2154";

	public static SimpleFeatureBuilder getBasicSchema(String name) {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode(epsg));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add(Collec.getDefaultGeomName(), Polygon.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public static SimpleFeatureBuilder getBasicSchemaID(String name) {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode(epsg));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add(Collec.getDefaultGeomName(), Polygon.class);
		sfTypeBuilder.add("id", Integer.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public static SimpleFeatureBuilder getBasicSchemaMultiPolygon(String name) {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode(epsg));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add(Collec.getDefaultGeomName(), MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public static SimpleFeatureBuilder getMUPAmenitySchema(String name) {
		SimpleFeatureTypeBuilder PointSfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			PointSfTypeBuilder.setCRS(CRS.decode(epsg));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		PointSfTypeBuilder.setName(name);
		PointSfTypeBuilder.add(Collec.getDefaultGeomName(), Point.class);
		PointSfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		PointSfTypeBuilder.add("TYPE", String.class);
		PointSfTypeBuilder.add("LEVEL", Integer.class);
		SimpleFeatureType pointFeatureType = PointSfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(pointFeatureType);
	}

	public static SimpleFeatureBuilder getMUPRoadSchema() {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.setName("road");
		try {
			sfTypeBuilder.setCRS(CRS.decode(epsg));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.add(Collec.getDefaultGeomName(), MultiLineString.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		sfTypeBuilder.add("SPEED", Integer.class);
		sfTypeBuilder.add("NATURE", String.class);
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public static SimpleFeatureBuilder getASCommunitySchema() {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		sfTypeBuilder.setName("testType");
		try {
			sfTypeBuilder.setCRS(CRS.decode(epsg));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
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

	public static String getEpsg() {
		return epsg;
	}

	public static void setEpsg(String epsg) {
		Schemas.epsg = epsg;
	}
}
