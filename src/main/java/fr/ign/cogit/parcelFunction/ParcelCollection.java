package fr.ign.cogit.parcelFunction;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.GTFunctions.Schemas;
import fr.ign.cogit.GTFunctions.Vectors;

public class ParcelCollection {

//	public static void main(String[] args) throws Exception {
//
//	}
	
	/**
	 * add a given collection of parcels to another collection of parcel, for which the schema is kept. 
	 * @param parcelIn : Parcels that receive the other parcels
	 * @param parcelAdd : Parcel to add
	 * @return
	 */
	public static DefaultFeatureCollection addAllParcels(SimpleFeatureCollection parcelIn, SimpleFeatureCollection parcelAdd) {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		result.addAll(parcelIn);
		SimpleFeatureIterator parcelAddIt = parcelAdd.features();
		try {
			while (parcelAddIt.hasNext()) {
				SimpleFeature featAdd = parcelAddIt.next();
				SimpleFeatureBuilder fit = ParcelSchema.setSFBParcelAsASWithFeat(featAdd);
				result.add(fit.buildFeature(null));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelAddIt.close();
		}
		return result;
	}
	
	
	// public static SimpleFeatureCollection
	// completeParcelMissing(SimpleFeatureCollection parcelTot,
	// SimpleFeatureCollection parcelCuted)
	// throws NoSuchAuthorityCodeException, FactoryException {
	// DefaultFeatureCollection result = new DefaultFeatureCollection();
	// SimpleFeatureType schema = parcelTot.features().next().getFeatureType();
	// // result.addAll(parcelCuted);
	// SimpleFeatureIterator parcelCutedIt = parcelCuted.features();
	// try {
	// while (parcelCutedIt.hasNext()) {
	// SimpleFeature featCut = parcelCutedIt.next();
	// SimpleFeatureBuilder fit = GetFromGeom.setSFBParcelWithFeat(featCut, schema);
	// result.add(fit.buildFeature(null));
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// parcelCutedIt.close();
	// }
	//
	// SimpleFeatureIterator totIt = parcelTot.features();
	// try {
	// while (totIt.hasNext()) {
	// SimpleFeature featTot = totIt.next();
	// boolean add = true;
	// SimpleFeatureIterator cutIt = parcelCuted.features();
	// try {
	// while (cutIt.hasNext()) {
	// SimpleFeature featCut = cutIt.next();
	// if (((Geometry)
	// featTot.getDefaultGeometry()).buffer(0.1).contains(((Geometry)
	// featCut.getDefaultGeometry()))) {
	// add = false;
	// break;
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// cutIt.close();
	// }
	// if (add) {
	// SimpleFeatureBuilder fit = GetFromGeom.setSFBParcelWithFeat(featTot, schema);
	// result.add(fit.buildFeature(null));
	// }
	// }
	// } catch (Exception problem) {
	// problem.printStackTrace();
	// } finally {
	// totIt.close();
	// }
	//
	// return result;
	// }

	public static SimpleFeatureCollection completeParcelMissingWithOriginal(SimpleFeatureCollection parcelToComplete,
			SimpleFeatureCollection originalParcel) throws NoSuchAuthorityCodeException, FactoryException, IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		result.addAll(parcelToComplete);
		// List<String> codeParcelAdded = new ArrayList<String>();

		// SimpleFeatureType schema =
		// parcelToComplete.features().next().getFeatureType();

		// result.addAll(parcelCuted);

		SimpleFeatureIterator parcelToCompletetIt = parcelToComplete.features();
		try {
			while (parcelToCompletetIt.hasNext()) {
				SimpleFeature featToComplete = parcelToCompletetIt.next();
				Geometry geomToComplete = (Geometry) featToComplete.getDefaultGeometry();
				Geometry geomsOrigin = Vectors.unionSFC(Vectors.snapDatas(originalParcel, geomToComplete));
				if (!geomsOrigin.buffer(1).contains(geomToComplete)) {
					// System.out.println("this parcel has disapeard : " + geomToComplete);
					// SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featToComplete,
					// schema);
					// result.add(fit.buildFeature(null));
					// SimpleFeatureBuilder builder =
					// FromGeom.setSFBOriginalParcelWithFeat(featToComplete, schema);
					// result.add(builder.buildFeature(null));
					// codeParcelAdded.add(ParcelFonction.makeParcelCode(featToComplete));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelToCompletetIt.close();
		}

		// SimpleFeatureIterator parcelOriginal = originalParcel.features();
		// try {
		// while (parcelOriginal.hasNext()) {
		// SimpleFeature featOriginal = parcelOriginal.next();
		// Geometry geom = (Geometry) featOriginal.getDefaultGeometry();
		// Geometry geomToComplete =
		// Vectors.unionSFC(Vectors.snapDatas(parcelToComplete, geom.buffer(10)));
		// if (!geomToComplete.contains(geom.buffer(-1))) {
		// System.out.println(geomToComplete);
		// System.out.println();
		// SimpleFeatureBuilder builder =
		// FromGeom.setSFBOriginalParcelWithFeat(featOriginal, schema);
		// result.add(builder.buildFeature(null));
		// codeParcelAdded.add(ParcelFonction.makeParcelCode(featOriginal));
		// }
		// SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featOriginal,
		// schema);
		// result.add(fit.buildFeature(null));
		// }
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// parcelOriginal.close();
		// }

		return result;
	}
/**
 * @warning NOT SURE IT'S WORKING
 * @param parcelTot
 * @param parcelCuted
 * @param parcelToNotAdd
 * @return
 * @throws NoSuchAuthorityCodeException
 * @throws FactoryException
 * @throws IOException
 */
	public static SimpleFeatureCollection completeParcelMissing(SimpleFeatureCollection parcelTot, SimpleFeatureCollection parcelCuted,
			List<String> parcelToNotAdd) throws NoSuchAuthorityCodeException, FactoryException, IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureType schema = parcelTot.features().next().getFeatureType();
		// result.addAll(parcelCuted);
		SimpleFeatureIterator parcelCutedIt = parcelCuted.features();
		try {
			while (parcelCutedIt.hasNext()) {
				SimpleFeature featCut = parcelCutedIt.next();
				SimpleFeatureBuilder fit = ParcelSchema.setSFBParcelAsASWithFeat(featCut, schema);
				result.add(fit.buildFeature(null));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelCutedIt.close();
		}

		SimpleFeatureIterator totIt = parcelTot.features();
		try {
			while (totIt.hasNext()) {
				SimpleFeature featTot = totIt.next();
				boolean add = true;
				for (String code : parcelToNotAdd) {
					if (featTot.getAttribute("CODE").equals(code)) {
						add = false;
						break;
					}
				}
				if (add) {
					SimpleFeatureBuilder fit = ParcelSchema.setSFBParcelAsASWithFeat(featTot, schema);
					result.add(fit.buildFeature(null));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			totIt.close();
		}
		return result.collection();
	}
	
	/**
	 * method that compares two set of parcels and sort the reference plan parcels between the ones that changed and the ones that doesn't We compare the parcels area of the
	 * reference parcel to the ones that are intersected. If they are similar with a 7% error rate, we conclude that they are the same.
	 * 
	 * @param parcelRefFile
	 *            : the reference parcel plan
	 * @param parcelToSortFile
	 *            : the parcel plan to compare
	 * @param parcelOutFolder
	 *            : folder where are stored the two created shapefile
	 * @return Two shapefile - One that contains the reference parcels that have changed (change.shp) and one with the parcels that haven't changed (noChange.shp)
	 * @throws IOException
	 */
	public static void markDiffParcel(File parcelRefFile, File parcelToSortFile, File parcelOutFolder) throws IOException {
		ShapefileDataStore sds = new ShapefileDataStore(parcelToSortFile.toURI().toURL());
		SimpleFeatureCollection parcelToSort = sds.getFeatureSource().getFeatures();

		ShapefileDataStore sdsRef = new ShapefileDataStore(parcelRefFile.toURI().toURL());
		SimpleFeatureCollection parcelRef = sdsRef.getFeatureSource().getFeatures();
		SimpleFeatureIterator itRef = parcelRef.features();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		PropertyName pName = ff.property(parcelRef.getSchema().getGeometryDescriptor().getLocalName());

		DefaultFeatureCollection same = new DefaultFeatureCollection();
		DefaultFeatureCollection notSame = new DefaultFeatureCollection();
		DefaultFeatureCollection polygonIntersection = new DefaultFeatureCollection();


		try {
			while (itRef.hasNext()) {
				SimpleFeature pRef = itRef.next();
				Geometry geomPRef = (Geometry) pRef.getDefaultGeometry();
				boolean not = true;
				// I THOUGHT THAT WOULD IMPROVE PERFORMANCES, BUT IT DOESN'T AT ALL
//				SimpleFeatureIterator itPToSort = parcelToSort.subCollection(ff.bbox(pName, pRef.getBounds())).features();
//				try {
//					while (itPToSort.hasNext()) {
//						SimpleFeature pToSort = itPToSort.next();
//						if (GeometryPrecisionReducer.reduce((geomPRef), new PrecisionModel(1)).equals(
//								GeometryPrecisionReducer.reduce(((Geometry) pToSort.getDefaultGeometry()).buffer(0), new PrecisionModel(1)))) {
//							same.add(pRef);
//							not = false;
//							break;
//						}
//					}
//				} catch (Exception problem) {
//					problem.printStackTrace();
//				} finally {
//					itPToSort.close();
//				}
				if (not) {
					// seek if it is close to (as tiny geometry changes
					SimpleFeatureCollection z = parcelToSort.subCollection(ff.intersects(pName, ff.literal(geomPRef)));
					SimpleFeatureIterator itPToSort = z.features();
					double geomArea = geomPRef.getArea();
					try {
						while (itPToSort.hasNext()) {
							SimpleFeature pToSort = itPToSort.next();
							Geometry geomPToSort = (Geometry) pToSort.getDefaultGeometry();
							double inter = geomPRef.intersection(geomPToSort).getArea();
							if (inter > 0.93 * geomArea && inter < 1.07 * geomArea) {
								same.add(pRef);
								not = false;
								break;
							}
						}
					} catch (Exception problem) {
						problem.printStackTrace();
					} finally {
						itPToSort.close();
					}
					if (not) {
						notSame.add(pRef);
						SimpleFeatureBuilder intersecPolygon = Schemas.getBasicSchemaMultiPolygon("intersectionPolygon");
						intersecPolygon.set("the_geom", ((Geometry) pRef.getDefaultGeometry()).buffer(-2));
						polygonIntersection.add(intersecPolygon.buildFeature(null));
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itRef.close();
		}
		sds.dispose();
		sdsRef.dispose();
		Vectors.exportSFC(same, new File(parcelOutFolder, "same.shp"));
		Vectors.exportSFC(notSame, new File(parcelOutFolder, "notSame.shp"));
		Vectors.exportSFC(polygonIntersection, new File(parcelOutFolder, "polygonIntersection.shp"));

	}
	
	public static List<String> dontAddParcel(List<String> parcelToNotAdd, SimpleFeatureCollection bigZoned) {

		SimpleFeatureIterator feat = bigZoned.features();
		try {
			while (feat.hasNext()) {
				parcelToNotAdd.add((String) feat.next().getAttribute("CODE"));
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			feat.close();
		}
		return parcelToNotAdd;
	}
}
