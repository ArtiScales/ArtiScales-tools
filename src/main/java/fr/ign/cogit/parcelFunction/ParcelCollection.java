package fr.ign.cogit.parcelFunction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

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

import fr.ign.cogit.geoToolsFunctions.Schemas;
import fr.ign.cogit.geoToolsFunctions.vectors.Collec;
import fr.ign.cogit.geoToolsFunctions.vectors.Geom;

public class ParcelCollection {

//	public static void main(String[] args) throws Exception {
//		ShapefileDataStore sds = new ShapefileDataStore(new File("/tmp/lala.shp").toURI().toURL());
//		SimpleFeatureCollection sfc = sds.getFeatureSource().getFeatures();
////		long startTime2 = System.nanoTime();
////		SimpleFeatureCollection parcelsUnsorted2 = Vectors.sortSFCWithArea(sfc);
////		long stopTime2 = System.nanoTime();
////		System.out.println("two took "+(stopTime2 - startTime2));
//		mergeTooSmallParcels(sfc,100);
//	}
	
	/**
	 * This algorithm merges parcels when they are under an area threshold. 
	 * It seek the surrounding parcel that share the largest side with the small parcel and merge their geometries. Parcel must touch at least. 
	 * If no surrounding parcels are found touching (or intersecting) the small parcel, the parcel is deleted and left as a public space.  
	 * Attributes from the large parcel are kept. 
	 * TODO apply that step to Parcel Manager algorithms 
	 * @param parcels SimpleFeature collection to check every parcels 
	 * @param minimalParcelSize threshold which parcels are under to be merged
	 * @return
	 * @throws FactoryException 
	 * @throws NoSuchAuthorityCodeException 
	 * @throws IOException 
	 */
	public static SimpleFeatureCollection mergeTooSmallParcels(SimpleFeatureCollection parcelsUnsorted, int minimalParcelSize)
			throws NoSuchAuthorityCodeException, FactoryException, IOException {
		SimpleFeatureBuilder build = new SimpleFeatureBuilder(parcelsUnsorted.getSchema());
		List<Integer> sizeResults = new ArrayList<Integer>();
		SimpleFeatureCollection result = recursiveMergeTooSmallParcel(parcelsUnsorted, minimalParcelSize, build);
		sizeResults.add(result.size());
		System.out.println("Merging too small parcels");
		System.out.println("OG size:"+parcelsUnsorted.size());
		System.out.println(sizeResults);
		do {
			// recursive application of the merge algorithm to merge little parcels to big ones one-by-one
			result = recursiveMergeTooSmallParcel(result, minimalParcelSize, build);
			sizeResults.add(result.size());
			System.out.println(sizeResults);
		}
		// while parcels are still getting merged, we run the recursive algorithm
		while (!sizeResults.get(sizeResults.size() - 1).equals(sizeResults.get(sizeResults.size() - 2)));
		return result;
	}
	
	private static SimpleFeatureCollection recursiveMergeTooSmallParcel(SimpleFeatureCollection parcelsUnsorted, int minimalParcelSize,
			SimpleFeatureBuilder build) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		// we sort the parcel collection to process the smallest parcels in first
		List<String> ids = new ArrayList<String>();
		
		// dead attempt to use dedicaded sorting objects
		// SimpleFeatureCollection parcels = Vectors.sortSFCWithArea(parcelsUnsorted);
		// SimpleFeatureIterator parcelIter = parcels.features();
		// FilterFactory2 ff = FeatureUtilities.DEFAULT_FILTER_FACTORY;
		// Function function = new AreaFunction();
		// SortBy[] s = {ff.sort(function.getName(), SortOrder.ASCENDING)};
		//// Expression expr = ff.function(function., ff.property("the_geom"));
		// final PropertyName propertyName = (PropertyName) function; //utopic
		//// final PropertyName propertyName = ff.arithmeticOperators(true, function); //that isn't the same function object...
		//
		//// SortBy[] sort = { new SortByImpl(expr, SortOrder.ASCENDING) };
		// SortBy[] sort = { new SortByImpl(propertyName, SortOrder.ASCENDING) };
		// SortedFeatureIterator parcelIt = new SortedFeatureIterator(parcelIter, parcels.getSchema(), sort, parcels.size());
		
		//easy hack to sort 
		SortedMap<Double, SimpleFeature> index = new TreeMap<>();
		try (SimpleFeatureIterator itr = parcelsUnsorted.features()){
			while (itr.hasNext()) {
				SimpleFeature feature = itr.next();
				double area = ((Geometry) feature.getDefaultGeometry()).getArea();
				index.put(area, feature);
			}
		}

		for (Entry<Double, SimpleFeature> entry : index.entrySet()) {
			SimpleFeature feat = entry.getValue();
			// if the parcel has already been merged with a smaller one, we skip (and we made the hypotheses that a merged parcel will always be bigger than the threshold)
			if (ids.contains(feat.getID())) {
				continue;
			}
			Geometry geom = (Geometry) feat.getDefaultGeometry();
			if (geom.getArea() < minimalParcelSize) {
				// System.out.println(feat.getID() + " is too small");
				DefaultFeatureCollection intersect = new DefaultFeatureCollection();
				Arrays.stream(parcelsUnsorted.toArray(new SimpleFeature[0])).forEach(interParcel -> {
					if (((Geometry) interParcel.getDefaultGeometry()).intersects(geom) && !interParcel.getID().equals(feat.getID())) {
						intersect.add(interParcel);
					}
				});
				if (intersect.size() > 0) {
					// System.out.println(intersect.size() + " intersecting");
					// if the tiny parcel intersects a bigger parcel, we seek the longest side to which parcel could be incorporated
					HashMap<String, Double> repart = new HashMap<String, Double>();
					Arrays.stream(intersect.toArray(new SimpleFeature[0])).forEach(interParcel -> {
						repart.put(interParcel.getID(), ((Geometry) interParcel.getDefaultGeometry()).intersection(geom.buffer(1)).getArea());
					});
					// we sort to place the biggest intersecting parcel in first
					List<Entry<String, Double>> entryList = new ArrayList<Entry<String, Double>>(repart.entrySet());
					Collections.sort(entryList, new Comparator<Entry<String, Double>>() {
						@Override
						public int compare(Entry<String, Double> obj1, Entry<String, Double> obj2) {
							return obj2.getValue().compareTo(obj1.getValue());
						}
					});
					String idToMerge = entryList.get(0).getKey();
//					System.out.println("idToMerge: " + idToMerge);
					// if the big parcel has already been merged with a small parcel, we skip it and will return to that small parcel in a future iteration
					if (ids.contains(idToMerge)) {
						result.add(feat);
						continue;
					}
					ids.add(idToMerge);
					// we now merge geometries and copy attributes to the new
					List<Geometry> lG = new ArrayList<Geometry>();
					lG.add(geom);
					Arrays.stream(intersect.toArray(new SimpleFeature[0])).forEach(thaParcel -> {
						if (thaParcel.getID().equals(idToMerge)) {
							build.addAll(thaParcel.getAttributes());
							lG.add((Geometry) thaParcel.getDefaultGeometry());
						}
					});
					build.set("the_geom", Geom.unionGeom(lG));
					SimpleFeature f = build.buildFeature(idToMerge);
					result.add(f);
				}
				// no else - if the small parcel doesn't touch any other parcels, we left it as a blank space and will be left as a public space
			} else {
				result.add(feat);
			}
		}
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// parcelIt.close();
		// }
		return result;
	}
	
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
	
//	/**
//	 * @FIXME fix that 
//	 * @param parcelToComplete
//	 * @param originalParcel
//	 * @return
//	 * @throws NoSuchAuthorityCodeException
//	 * @throws FactoryException
//	 * @throws IOException
//	 */
//	public static SimpleFeatureCollection completeParcelMissingWithOriginal(SimpleFeatureCollection parcelToComplete,
//			SimpleFeatureCollection originalParcel) throws NoSuchAuthorityCodeException, FactoryException, IOException {
//		DefaultFeatureCollection result = new DefaultFeatureCollection();
//		result.addAll(parcelToComplete);
//		// List<String> codeParcelAdded = new ArrayList<String>();
//
//		// SimpleFeatureType schema =
//		// parcelToComplete.features().next().getFeatureType();
//
//		// result.addAll(parcelCuted);
//
//		SimpleFeatureIterator parcelToCompletetIt = parcelToComplete.features();
//		try {
//			while (parcelToCompletetIt.hasNext()) {
//				SimpleFeature featToComplete = parcelToCompletetIt.next();
//				Geometry geomToComplete = (Geometry) featToComplete.getDefaultGeometry();
//				Geometry geomsOrigin = Vectors.unionSFC(Vectors.snapDatas(originalParcel, geomToComplete));
//				if (!geomsOrigin.buffer(1).contains(geomToComplete)) {
//					// System.out.println("this parcel has disapeard : " + geomToComplete);
//					// SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featToComplete,
//					// schema);
//					// result.add(fit.buildFeature(null));
//					// SimpleFeatureBuilder builder =
//					// FromGeom.setSFBOriginalParcelWithFeat(featToComplete, schema);
//					// result.add(builder.buildFeature(null));
//					// codeParcelAdded.add(ParcelFonction.makeParcelCode(featToComplete));
//				}
//			}
//		} catch (Exception problem) {
//			problem.printStackTrace();
//		} finally {
//			parcelToCompletetIt.close();
//		}
//
//		// SimpleFeatureIterator parcelOriginal = originalParcel.features();
//		// try {
//		// while (parcelOriginal.hasNext()) {
//		// SimpleFeature featOriginal = parcelOriginal.next();
//		// Geometry geom = (Geometry) featOriginal.getDefaultGeometry();
//		// Geometry geomToComplete =
//		// Vectors.unionSFC(Vectors.snapDatas(parcelToComplete, geom.buffer(10)));
//		// if (!geomToComplete.contains(geom.buffer(-1))) {
//		// System.out.println(geomToComplete);
//		// System.out.println();
//		// SimpleFeatureBuilder builder =
//		// FromGeom.setSFBOriginalParcelWithFeat(featOriginal, schema);
//		// result.add(builder.buildFeature(null));
//		// codeParcelAdded.add(ParcelFonction.makeParcelCode(featOriginal));
//		// }
//		// SimpleFeatureBuilder fit = FromGeom.setSFBParcelWithFeat(featOriginal,
//		// schema);
//		// result.add(fit.buildFeature(null));
//		// }
//		// } catch (Exception problem) {
//		// problem.printStackTrace();
//		// } finally {
//		// parcelOriginal.close();
//		// }
//
//		return result;
//	}
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
		Collec.exportSFC(same, new File(parcelOutFolder, "same.shp"));
		Collec.exportSFC(notSame, new File(parcelOutFolder, "notSame.shp"));
		Collec.exportSFC(polygonIntersection, new File(parcelOutFolder, "polygonIntersection.shp"));

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
