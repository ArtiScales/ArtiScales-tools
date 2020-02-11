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
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import fr.ign.cogit.GTFunctions.Vectors;

public class ParcelCollection {

	public static void main(String[] args) throws Exception {
		markDiffParcel(new File("/tmp/brie98.shp"),new File("/tmp/brie12.shp"), new File("/tmp/"));
//		markDiffParcel(new File("/tmp/a.shp"),new File("/tmp/b.shp"), new File("/tmp/"));

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
	 * method that compares two set of parcels and export only the ones that are in common 
	 * 
	 * @param parcelRef
	 * @param parcelToSort
	 * @param parcelOutFolder
	 * @throws IOException
	 */
	public static void markDiffParcel(File parcelRef, File parcelToSort, File parcelOutFolder) throws IOException {
		ShapefileDataStore sds = new ShapefileDataStore(parcelToSort.toURI().toURL());
		SimpleFeatureCollection parcelUnclean = sds.getFeatureSource().getFeatures();

		ShapefileDataStore sdsRef = new ShapefileDataStore(parcelRef.toURI().toURL());
		SimpleFeatureCollection parcelClean = sdsRef.getFeatureSource().getFeatures();
		SimpleFeatureIterator itClean = parcelClean.features();

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		PropertyName pName = ff.property(parcelUnclean.getSchema().getGeometryDescriptor().getLocalName());

		DefaultFeatureCollection same = new DefaultFeatureCollection();
		DefaultFeatureCollection notSame = new DefaultFeatureCollection();

		int i = 1;
		try {
			while (itClean.hasNext()) {
				SimpleFeature clean = itClean.next();
				boolean not = true;
				SimpleFeatureIterator itUnclean = parcelUnclean.subCollection(ff.bbox(pName, clean.getBounds())).features();
				try {
					while (itUnclean.hasNext()) {
						SimpleFeature unclean = itUnclean.next();
						if (GeometryPrecisionReducer.reduce(((Geometry) clean.getDefaultGeometry()).buffer(0), new PrecisionModel(1)).equals(
								GeometryPrecisionReducer.reduce(((Geometry) unclean.getDefaultGeometry()).buffer(0), new PrecisionModel(1)))) {
							same.add(clean);
							not = false;
							break;
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itUnclean.close();
				}
				if (not) {
					// seek if it is close to (as tiny geometry changes
					SimpleFeatureIterator itUnclean2 = parcelUnclean
							.subCollection(ff.crosses(pName, ff.literal((Geometry) clean.getDefaultGeometry()))).features();
					try {
						while (itUnclean2.hasNext()) {
							SimpleFeature unclean = itUnclean2.next();
							if (Math.abs(((Geometry) clean.getDefaultGeometry()).intersection(((Geometry) unclean.getDefaultGeometry())).getArea()) < 10) {
								same.add(clean);
								not = false;
								break;
							}
						}
					} catch (Exception problem) {
						problem.printStackTrace();
					} finally {
						itUnclean2.close();
					}
					if(not) {
						notSame.add(clean);
					}
				}
				System.out.println(i++ + " on " + parcelClean.size());
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itClean.close();
		}
		sds.dispose();
		sdsRef.dispose();
		Vectors.exportSFC(same, new File(parcelOutFolder, "same.shp"));
		Vectors.exportSFC(notSame, new File(parcelOutFolder, "notSame.shp"));

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
