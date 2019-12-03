package fr.ign.cogit.parcelFunction;

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

public class ParcelSchema {

	/////////////////////
	/////////////////////
	//// FrenchParcel Schemas : basic parcels schema used in the french IGN norm
	/////////////////////
	/////////////////////

	public static SimpleFeatureBuilder getSFBFrenchParcel() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("CODE", String.class);
		sfTypeBuilder.add("CODE_DEP", String.class);
		sfTypeBuilder.add("CODE_COM", String.class);
		sfTypeBuilder.add("COM_ABS", String.class);
		sfTypeBuilder.add("SECTION", String.class);
		sfTypeBuilder.add("NUMERO", String.class);

		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	public static SimpleFeatureBuilder setSFBFrenchParcelWithFeat(SimpleFeature feat, SimpleFeatureType schema) {
		SimpleFeatureBuilder finalParcelBuilder = new SimpleFeatureBuilder(schema);
		finalParcelBuilder.set(schema.getGeometryDescriptor().getName().toString(), (Geometry) feat.getDefaultGeometry());
		finalParcelBuilder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
		finalParcelBuilder.set("CODE_COM", feat.getAttribute("CODE_COM"));
		finalParcelBuilder.set("COM_ABS", feat.getAttribute("COM_ABS"));
		finalParcelBuilder.set("SECTION", feat.getAttribute("SECTION"));
		finalParcelBuilder.set("NUMERO", feat.getAttribute("NUMERO"));
		return finalParcelBuilder;
	}

	public static SimpleFeatureBuilder getSFBFrenchParcelSplit() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("CODE", String.class);
		sfTypeBuilder.add("CODE_DEP", String.class);
		sfTypeBuilder.add("CODE_COM", String.class);
		sfTypeBuilder.add("COM_ABS", String.class);
		sfTypeBuilder.add("SECTION", String.class);
		sfTypeBuilder.add("NUMERO", String.class);
		sfTypeBuilder.add("SPLIT", String.class);

		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	public static SimpleFeatureBuilder setSFBFrenchParcelSplitWithFeat(SimpleFeature feat, SimpleFeatureType schema, int split) {
		return fillSFBFrenchParcelSplitWithFeat(feat, new SimpleFeatureBuilder(schema), schema.getGeometryDescriptor().getName().toString(), split);
	}

	public static SimpleFeatureBuilder fillSFBFrenchParcelSplitWithFeat(SimpleFeature feat, SimpleFeatureBuilder builder, String geomName,
			int split) {
		return fillSFBFrenchParcelSplitWithFeat(feat, builder, geomName, (Geometry) feat.getDefaultGeometry(), split);

	}

	public static SimpleFeatureBuilder fillSFBFrenchParcelSplitWithFeat(SimpleFeature feat, SimpleFeatureBuilder builder, String geomName,
			Geometry geom, int split) {
		builder.set(geomName, geom);
		builder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
		builder.set("CODE_COM", feat.getAttribute("CODE_COM"));
		builder.set("COM_ABS", feat.getAttribute("COM_ABS"));
		builder.set("SECTION", feat.getAttribute("SECTION"));
		builder.set("NUMERO", feat.getAttribute("NUMERO"));
		builder.set("SPLIT", split);

		return builder;
	}

	/////////////////////
	/////////////////////
	//// AsAS Schemas : basic parcels schema used in ArtiScales
	/////////////////////
	/////////////////////
	public static SimpleFeatureBuilder getSFBParcelAsAS() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("CODE", String.class);
		sfTypeBuilder.add("CODE_DEP", String.class);
		sfTypeBuilder.add("CODE_COM", String.class);
		sfTypeBuilder.add("COM_ABS", String.class);
		sfTypeBuilder.add("SECTION", String.class);
		sfTypeBuilder.add("NUMERO", String.class);
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("eval", String.class);
		sfTypeBuilder.add("DoWeSimul", String.class);
		sfTypeBuilder.add("IsBuild", Boolean.class);
		sfTypeBuilder.add("U", Boolean.class);
		sfTypeBuilder.add("AU", Boolean.class);
		sfTypeBuilder.add("NC", Boolean.class);

		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	public static SimpleFeatureBuilder setSFBParcelAsASWithFeat(SimpleFeature feat) {
		return setSFBParcelAsASWithFeat(feat, feat.getFeatureType());
	}

	public static SimpleFeatureBuilder setSFBParcelAsASWithFeat(SimpleFeature feat, SimpleFeatureType schema) {
		SimpleFeatureBuilder finalParcelBuilder = new SimpleFeatureBuilder(schema);
		return finalParcelBuilder = fillSFBParcelAsASWithFeat(feat, finalParcelBuilder, schema);
	}

	public static SimpleFeatureBuilder fillSFBParcelAsASWithFeat(SimpleFeature feat, SimpleFeatureBuilder builder, SimpleFeatureType schema) {
		builder.set(schema.getGeometryDescriptor().getName().toString(), (Geometry) feat.getDefaultGeometry());
		builder.set("CODE", feat.getAttribute("CODE"));
		builder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
		builder.set("CODE_COM", feat.getAttribute("CODE_COM"));
		builder.set("COM_ABS", feat.getAttribute("COM_ABS"));
		builder.set("SECTION", feat.getAttribute("SECTION"));
		builder.set("NUMERO", feat.getAttribute("NUMERO"));
		builder.set("INSEE", feat.getAttribute("INSEE"));
		builder.set("eval", feat.getAttribute("eval"));
		builder.set("DoWeSimul", feat.getAttribute("DoWeSimul"));
		builder.set("IsBuild", feat.getAttribute("IsBuild"));
		builder.set("U", feat.getAttribute("U"));
		builder.set("AU", feat.getAttribute("AU"));
		builder.set("NC", feat.getAttribute("NC"));
		return builder;
	}

	public static SimpleFeatureBuilder setSFBParcelAsASWithFrenchParcelFeat(SimpleFeature feat) {
		return setSFBParcelAsASWithFrenchParcelFeat(feat, feat.getFeatureType(), feat.getFeatureType().getGeometryDescriptor().getName().toString());
	}

	public static SimpleFeatureBuilder setSFBParcelAsASWithFrenchParcelFeat(SimpleFeature feat, SimpleFeatureType schema) {
		return setSFBParcelAsASWithFrenchParcelFeat(feat, schema, schema.getGeometryDescriptor().getName().toString());
	}

	public static SimpleFeatureBuilder setSFBParcelAsASWithFrenchParcelFeat(SimpleFeature feat, SimpleFeatureType schema, String geometryOutputName) {
		SimpleFeatureBuilder finalParcelBuilder = new SimpleFeatureBuilder(schema);
		finalParcelBuilder.set(geometryOutputName, (Geometry) feat.getDefaultGeometry());
		finalParcelBuilder.set("CODE", ParcelAttribute.makeParcelCode(feat));
		finalParcelBuilder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
		finalParcelBuilder.set("CODE_COM", feat.getAttribute("CODE_COM"));
		finalParcelBuilder.set("COM_ABS", feat.getAttribute("COM_ABS"));
		finalParcelBuilder.set("SECTION", feat.getAttribute("SECTION"));
		finalParcelBuilder.set("NUMERO", feat.getAttribute("NUMERO"));
		finalParcelBuilder.set("INSEE", ParcelAttribute.makeINSEECode(feat));
		finalParcelBuilder.set("eval", "0");
		finalParcelBuilder.set("DoWeSimul", "false");
		finalParcelBuilder.set("IsBuild", "false");
		finalParcelBuilder.set("U", "false");
		finalParcelBuilder.set("AU", "false");
		finalParcelBuilder.set("NC", "false");
		return finalParcelBuilder;
	}

	/////////////////////
	/////////////////////
	//// AsASSplit Schemas : parcels schema used in ArtiScales for marking which parcel is cut (or merged) on parcel reshaping process
	/////////////////////
	/////////////////////
	public static SimpleFeatureBuilder getSFBParcelAsASSplit() throws NoSuchAuthorityCodeException, FactoryException {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:2154");
		sfTypeBuilder.setName("testType");
		sfTypeBuilder.setCRS(sourceCRS);
		sfTypeBuilder.add("the_geom", Polygon.class);
		sfTypeBuilder.setDefaultGeometry("the_geom");
		sfTypeBuilder.add("CODE", String.class);
		sfTypeBuilder.add("CODE_DEP", String.class);
		sfTypeBuilder.add("CODE_COM", String.class);
		sfTypeBuilder.add("COM_ABS", String.class);
		sfTypeBuilder.add("SECTION", String.class);
		sfTypeBuilder.add("NUMERO", String.class);
		sfTypeBuilder.add("INSEE", String.class);
		sfTypeBuilder.add("eval", String.class);
		sfTypeBuilder.add("DoWeSimul", String.class);
		sfTypeBuilder.add("SPLIT", Integer.class);
		sfTypeBuilder.add("IsBuild", Boolean.class);
		sfTypeBuilder.add("U", Boolean.class);
		sfTypeBuilder.add("AU", Boolean.class);
		sfTypeBuilder.add("NC", Boolean.class);

		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	public static SimpleFeatureBuilder setSFBParcelAsASSplitWithFeat(SimpleFeature feat, SimpleFeatureType schema) {
		SimpleFeatureBuilder finalParcelBuilder = new SimpleFeatureBuilder(schema);
		finalParcelBuilder = fillSFBParcelAsASSplitWithFeat(feat, finalParcelBuilder, schema);
		return finalParcelBuilder;
	}

	public static SimpleFeatureBuilder fillSFBParcelAsASSplitWithFeat(SimpleFeature feat, SimpleFeatureBuilder builder, SimpleFeatureType schema) {
		return fillSFBParcelAsASSplitWithFeat(feat, builder, schema.getGeometryDescriptor().getName().toString(), (int) feat.getAttribute("SPLIT"));
	}

	public static SimpleFeatureBuilder fillSFBParcelAsASSplitWithFeat(SimpleFeature feat, SimpleFeatureBuilder builder, String geomName, int split) {
		builder.set(geomName, (Geometry) feat.getDefaultGeometry());
		builder.set("CODE", feat.getAttribute("CODE"));
		builder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
		builder.set("CODE_COM", feat.getAttribute("CODE_COM"));
		builder.set("COM_ABS", feat.getAttribute("COM_ABS"));
		builder.set("SECTION", feat.getAttribute("SECTION"));
		builder.set("NUMERO", feat.getAttribute("NUMERO"));
		builder.set("INSEE", feat.getAttribute("INSEE"));
		builder.set("eval", feat.getAttribute("eval"));
		builder.set("DoWeSimul", feat.getAttribute("DoWeSimul"));
		builder.set("SPLIT", split);
		builder.set("IsBuild", feat.getAttribute("IsBuild"));
		builder.set("U", feat.getAttribute("U"));
		builder.set("AU", feat.getAttribute("AU"));
		builder.set("NC", feat.getAttribute("NC"));
		return builder;
	}
}