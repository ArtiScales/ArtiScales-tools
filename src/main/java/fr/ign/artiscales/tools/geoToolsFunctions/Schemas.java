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

import java.util.List;
import java.util.stream.Collectors;

public class Schemas {
    /**
     * Class containing methods to deal with Schemas and/or SimpleFeatureBuilders
     */
    private static String epsg = "EPSG:2154";

    /**
     * Get a schema for {@link Polygon} with no {@link org.opengis.feature.Attribute}
     *
     * @param name Name of the schema
     * @return empty builder
     */
    public static SimpleFeatureBuilder getBasicPolygonSchema(String name) {
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

    /**
     * Get basic schemas for different geometries.
     *
     * @param name          name of the future feature collection
     * @param geometryClass name of the geometry type. For now only Polygon, MultiPolygon and MultiLineString implemented.
     * @return a basic SFB with no attribute
     */
    public static SimpleFeatureBuilder getBasicSchema(String name, String geometryClass) {
        if (geometryClass.toLowerCase().contains("multipolygon")) {
            return getBasicMultiPolygonSchema(name);
        } else if (geometryClass.toLowerCase().contains("polygon")) {
            return getBasicPolygonSchema(name);
        } else if (geometryClass.toLowerCase().contains("multilinestring")) {
            return getBasicMLSSchema(name);
        }
        throw new IllegalArgumentException("getBasicSchema : geometry type" + geometryClass + " unimplemented");
    }


    /**
     * Get a schema for {@link MultiLineString} with no {@link org.opengis.feature.Attribute}
     *
     * @param name Name of the schema
     * @return empty builder
     */
    public static SimpleFeatureBuilder getBasicMLSSchema(String name) {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode(epsg));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName(name);
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), MultiLineString.class);
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

    public static SimpleFeatureBuilder getBasicMultiPolygonSchema(String name) {
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
     *
     * @param sfcIn
     * @param attributeName name of the attribute
     * @return the SFC with a float column
     */
    public static SimpleFeatureBuilder addFloatColToSFB(SimpleFeatureCollection sfcIn, String attributeName) {
        return addColToSFB(sfcIn, attributeName, Float.class);
    }

    /**
     * Return a {@link SimpleFeatureBuilder} out of an existing {@link org.geotools.data.simple.SimpleFeatureCollection} and add a {@link Float} type attribute.
     *
     * @param sfcIn
     * @param attributeName name of the attribute
     * @return the SFC with a float column
     */
    public static SimpleFeatureBuilder addIntColToSFB(SimpleFeatureCollection sfcIn, String attributeName) {
        return addColToSFB(sfcIn, attributeName, Integer.class);
    }

    /**
     * Return a {@link SimpleFeatureBuilder} out of an existing {@link org.geotools.data.simple.SimpleFeatureCollection} and add a type attribute.
     *
     * @param sfcIn
     * @param attributeName name of the attribute
     * @param c             Java class
     * @return the SFC with a float column
     */
    public static SimpleFeatureBuilder addColToSFB(SimpleFeatureCollection sfcIn, String attributeName, Class c) {
        String[] name = {attributeName};
        return addColsToSFB(sfcIn, name, c);
    }

    /**
     * Return a {@link SimpleFeatureBuilder} out of an existing {@link org.geotools.data.simple.SimpleFeatureCollection} and add multiple attributes. All the attributes must have the same type.
     *
     * @param sfcIn           collection with schema to copy
     * @param attributesNames name of the attributes (must be the same type)
     * @param c               Java class
     * @return the SFC with a float column
     */
    public static SimpleFeatureBuilder addColsToSFB(SimpleFeatureCollection sfcIn, String[] attributesNames, Class c) {
        SimpleFeatureType schema = sfcIn.getSchema();
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        for (AttributeDescriptor attr : schema.getAttributeDescriptors())
            sfTypeBuilder.add(attr);
        for (String attributeName : attributesNames)
            sfTypeBuilder.add(attributeName, c);
        sfTypeBuilder.setName(schema.getName());
        sfTypeBuilder.setCRS(schema.getCoordinateReferenceSystem());
        sfTypeBuilder.setDefaultGeometry(schema.getGeometryDescriptor().getLocalName());
        return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
    }

    /**
     * Set the attribute of the builder to theirs matching equivalent stored in a simpleFeature.
     * Catch if an attribute of the simpleFeature doesn't have a matching equivalant in the builder.
     *
     * @param sfb input builder
     * @param sf  input simpleFeature
     */
    public static void setFieldsToSFB(SimpleFeatureBuilder sfb, SimpleFeature sf) {
        for (AttributeDescriptor attr : sf.getFeatureType().getAttributeDescriptors())
            try {
                sfb.set(attr.getLocalName(), sf.getAttribute(attr.getLocalName()));
            } catch (IllegalArgumentException e) {
//                System.out.println("setFieldsToSFB : no argument "+attr.getLocalName());
            }
    }

    /**
     * Check if the given schema contains the given field name.
     *
     * @param schema             SimpleFeatureType schema
     * @param attributeFiledName Name of the field (must respect case)
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

    /**
     * Get name of attributes that are in every input SFCs
     *
     * @param sfcs input
     * @return the matching field names
     */
    public static List<String> getMatchingFields(List<SimpleFeatureCollection> sfcs) {
        List<String> result = sfcs.get(0).getSchema().getAttributeDescriptors().stream().map(AttributeDescriptor::getLocalName).collect(Collectors.toList());
        for (int i = 1; i < sfcs.size(); i++)
            sfcs.get(i).getSchema().getAttributeDescriptors().stream().filter(a -> !result.contains(a.getLocalName())).forEach(a -> result.remove(a.getLocalName()));
        return result;
    }
}
