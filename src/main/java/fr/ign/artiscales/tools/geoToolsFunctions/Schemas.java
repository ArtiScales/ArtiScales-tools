package fr.ign.artiscales.tools.geoToolsFunctions;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.data.simple.SimpleFeatureCollection;
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

public class Schemas {
	/**
	 * Class containing methods to deal with Schemas and/or SimpleFeatureBuilders
	 */
	private static String epsg = "EPSG:2154";

	public static SimpleFeatureBuilder getBasicSchema(String name) {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode(epsg));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName(name);
		sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Polygon.class);
		sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
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
		sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Polygon.class);
		sfTypeBuilder.add("id", Integer.class);
		sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
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
		sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
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
		PointSfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Point.class);
		PointSfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
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
		sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), MultiLineString.class);
		sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
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
		sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), MultiPolygon.class);
		sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
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

	/**
	 * Return a {@link SimpleFeatureBuilder} out of an existing {@link org.geotools.data.simple.SimpleFeatureCollection} and add a {@link Float} type attribute.
	 * @param sfcIn
	 * @param attributeName name of the attribute
	 * @return the SFC with a float column
	 */
	public static SimpleFeatureBuilder addFloatColToSFB(SimpleFeatureCollection sfcIn, String attributeName) {
		return addColToSFB(sfcIn, attributeName, Float.class);
	}

	/**
	 * Return a {@link SimpleFeatureBuilder} out of an existing {@link org.geotools.data.simple.SimpleFeatureCollection} and add a {@link Float} type attribute.
	 * @param sfcIn
	 * @param attributeName name of the attribute
	 * @return the SFC with a float column
	 */
	public static SimpleFeatureBuilder addIntColToSFB(SimpleFeatureCollection sfcIn, String attributeName) {
		return addColToSFB(sfcIn, attributeName, Integer.class);
	}

	/**
	 * Return a {@link SimpleFeatureBuilder} out of an existing {@link org.geotools.data.simple.SimpleFeatureCollection} and add a type attribute.
	 * @param sfcIn
	 * @param attributeName name of the attribute
	 * @param c Java class
	 * @return the SFC with a float column
	 */
	public static SimpleFeatureBuilder addColToSFB(SimpleFeatureCollection sfcIn, String attributeName, Class c) {
		SimpleFeatureType schema = sfcIn.getSchema();
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		for (AttributeDescriptor attr : schema.getAttributeDescriptors())
			sfTypeBuilder.add(attr);
		sfTypeBuilder.add(attributeName, c);
		sfTypeBuilder.setName(schema.getName());
		sfTypeBuilder.setCRS(schema.getCoordinateReferenceSystem());
		sfTypeBuilder.setDefaultGeometry(schema.getGeometryDescriptor().getLocalName());
		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	public static SimpleFeatureBuilder setFieldsToSFB(SimpleFeatureBuilder sfb, SimpleFeature sf){
		try  {
			for (int i = 0 ; i < sf.getFeatureType().getAttributeCount() ; i++)
				sfb.set(sf.getFeatureType().getType(i).getName(), sf.getAttribute(i));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sfb;
	}

	/**
	 * Check if the given schema contains the given field name.
	 *
	 * @param schema
	 *            SimpleFeatureType schema
	 * @param attributeFiledName
	 *            Name of the field (must respect case)
	 * @return true if the collec contains the field name, false otherwise
	 */
	public static boolean isSchemaContainsAttribute(SimpleFeatureType schema, String attributeFiledName) {
		return schema.getAttributeDescriptors().stream().anyMatch(s -> s.getName().toString().equals(attributeFiledName));
	}

	public static String getEpsg() {
		return epsg;
	}

	public static void setEpsg(String epsg) {
		Schemas.epsg = epsg;
	}
}
