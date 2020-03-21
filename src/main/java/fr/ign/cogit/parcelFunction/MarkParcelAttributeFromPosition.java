package fr.ign.cogit.parcelFunction;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.geoToolsFunctions.vectors.Collec;
import fr.ign.cogit.geoToolsFunctions.vectors.Geom;

public class MarkParcelAttributeFromPosition {
	
	static String markFieldName = "SPLIT";

	/**
	 * Mark a given number of parcel for the simulation. The selection is random but
	 * parcels must be bigger than a certain area threshold.
	 * 
	 * @param parcels        input parcel collection
	 * @param minSize        : minimal size of parcels to be selected
	 * @param nbParcelToMark : number of parcel wanted
	 * @return a random collection of parcel marked to be simulated.
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static SimpleFeatureCollection markRandomParcels(SimpleFeatureCollection parcels, int minSize,
			int nbParcelToMark) throws NoSuchAuthorityCodeException, FactoryException {
		return markRandomParcels(parcels, null, null, minSize, nbParcelToMark);
	}

	/**
	 * Mark a given number of parcel for the simulation. The selection is random but
	 * parcels must be bigger than a certain area threshold and must be contained is
	 * a given zoning type.
	 * 
	 * @param parcels        input parcel collection
	 * @param minSize        : minimal size of parcels to be selected
	 * @param zoningType     : type of the zoning plan to take into consideration
	 * @param zoningFile     : Shapefile containing the zoning plan
	 * @param nbParcelToMark : number of parcel wanted
	 * @return a random collection of parcel marked to be simulated.
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static SimpleFeatureCollection markRandomParcels(SimpleFeatureCollection parcels, String zoningType,
			File zoningFile, double minSize, int nbParcelToMark) throws NoSuchAuthorityCodeException, FactoryException {
		if (zoningFile != null && zoningType != null) {
			parcels = markParcelIntersectZoningType(parcels, zoningType, zoningFile);
		}
		List<SimpleFeature> list = Arrays.stream(parcels.toArray(new SimpleFeature[0])).filter(feat -> 
		((Geometry)feat.getDefaultGeometry()).getArea() > minSize).collect(Collectors.toList());
		Collections.shuffle(list);
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		while (nbParcelToMark > 0) {
			if (!list.isEmpty()) {
				result.add(list.remove(0));
			}
			nbParcelToMark--;
		}
	return parcels;
	}
	
	public static SimpleFeatureCollection markBuiltParcel(SimpleFeatureCollection parcels, File buildingFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {
		ShapefileDataStore sds = new ShapefileDataStore(buildingFile.toURI().toURL());
		SimpleFeatureCollection buildings = Collec.snapDatas(sds.getFeatureSource().getFeatures(), parcels);

		final SimpleFeatureType featureSchema = ParcelSchema.getSFBMinParcelSplit().getFeatureType();
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		Arrays.stream(parcels.toArray(new SimpleFeature[0])).forEach(feat -> {
			try {
				SimpleFeatureBuilder featureBuilder = ParcelSchema.getSFBMinParcelSplit();
				if (ParcelState.isAlreadyBuilt(buildings, feat, -1.0)) {
					featureBuilder = ParcelSchema.setSFBMinParcelSplitWithFeat(feat,featureBuilder, featureSchema, 1);
				} else {
					featureBuilder = ParcelSchema.setSFBMinParcelSplitWithFeat(feat,featureBuilder, featureSchema, 0);
				}
				result.add(featureBuilder.buildFeature(null));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		sds.dispose();
		return result.collection();
	}
	
	
	/**
	 * Mark parcels that intersects a given collection of polygons.
	 * The default field name containing the mark is "SPLIT" but it can be changed with the {@link #setMarkFieldName()} method.
	 * 
	 * @param parcels
	 *            : The collection of parcels to mark
	 * @param polygonIntersectionFile
	 *            : A shapefile containing the collection of polygons
	 * @return
	 * @throws IOException
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws Exception
	 */
	public static SimpleFeatureCollection markParcelIntersectPolygonIntersection(SimpleFeatureCollection parcels, File polygonIntersectionFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException {

		ShapefileDataStore sds = new ShapefileDataStore(polygonIntersectionFile.toURI().toURL());
		Geometry geomPolygonIntersection = Geom.unionSFC(Collec.snapDatas(sds.getFeatureSource().getFeatures(), parcels));

		final SimpleFeatureType featureSchema = ParcelSchema.getSFBMinParcelSplit().getFeatureType();
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		Arrays.stream(parcels.toArray(new SimpleFeature[0])).forEach(feat -> {
			try {
				SimpleFeatureBuilder featureBuilder = ParcelSchema.getSFBMinParcelSplit();
				if (((Geometry) feat.getDefaultGeometry()).intersects(geomPolygonIntersection)) {
					featureBuilder = ParcelSchema.setSFBMinParcelSplitWithFeat(feat,featureBuilder, featureSchema, 1);
				} else {
					featureBuilder = ParcelSchema.setSFBMinParcelSplitWithFeat(feat,featureBuilder, featureSchema, 0);
				}
				result.add(featureBuilder.buildFeature(null));
			} catch (FactoryException e) {
				e.printStackTrace();
			}
		});
		sds.dispose();
		return result.collection();
	}

	/**
	 * mark parcels that intersects a certain type of zoning.
	 * 
	 * @param parcels
	 * @param zoningType
	 *            : The big kind of the zoning (either not constructible (NC), urbanizable (U) or to be urbanize (TBU). Other keywords can be tolerate
	 * @param zoningFile
	 *            : A shapefile containing the zoning plan
	 * @return The same collection of parcels with the SPLIT field
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws IOException
	 * @throws Exception
	 */
	public static SimpleFeatureCollection markParcelIntersectZoningType(SimpleFeatureCollection parcels, String zoningType, File zoningFile) throws NoSuchAuthorityCodeException, FactoryException {
		final SimpleFeatureType featureSchema = ParcelSchema.getSFBMinParcelSplit().getFeatureType();
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		Arrays.stream(parcels.toArray(new SimpleFeature[0])).forEach(feat -> {
			SimpleFeatureBuilder featureBuilder;
			try {
				featureBuilder = ParcelSchema.getSFBMinParcelSplit();
				if (ParcelState.parcelInBigZone(zoningFile, feat).equals(zoningType)
						&& (feat.getFeatureType().getDescriptor(markFieldName) == null || feat.getAttribute(markFieldName).equals(1))) {
					featureBuilder = ParcelSchema.setSFBMinParcelSplitWithFeat(feat, featureBuilder, featureSchema, 1);
				} else {
					featureBuilder = ParcelSchema.setSFBMinParcelSplitWithFeat(feat, featureBuilder, featureSchema, 0);
				}
				result.add(featureBuilder.buildFeature(null));
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		return result;
	}

	public static SimpleFeatureCollection markParcelOfCommunityType(SimpleFeatureCollection parcels, String attribute, File communityFile)
			throws NoSuchAuthorityCodeException, FactoryException {
		return markParcelOfCommunity(parcels, "armature", attribute, communityFile);
	}

	public static SimpleFeatureCollection markParcelOfCommunityNumber(SimpleFeatureCollection parcels, String attribute, File communityFile)
			throws NoSuchAuthorityCodeException, FactoryException {
		return markParcelOfCommunity(parcels, "INSEE", attribute, communityFile);
	}

	public static SimpleFeatureCollection markParcelOfCommunity(SimpleFeatureCollection parcels, String fieldName, String attribute,
			File communityFile) throws NoSuchAuthorityCodeException, FactoryException {
		final SimpleFeatureType featureSchema = ParcelSchema.getSFBMinParcelSplit().getFeatureType();
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		Arrays.stream(parcels.toArray(new SimpleFeature[0])).forEach(feat -> {
			SimpleFeatureBuilder featureBuilder = ParcelSchema.setSFBMinParcelSplitWithFeat(feat, featureSchema);
			if (feat.getAttribute(fieldName).equals(attribute)) {
				featureBuilder.set(markFieldName, 1);
			} else {
				featureBuilder.set(markFieldName, 0);
			}
			result.add(featureBuilder.buildFeature(null));
		});
		return result;
	}

	public static String getMarkFieldName() {
		return markFieldName;
	}

	public static void setMarkFieldName(String markFieldName) {
		MarkParcelAttributeFromPosition.markFieldName = markFieldName;
	}
}
