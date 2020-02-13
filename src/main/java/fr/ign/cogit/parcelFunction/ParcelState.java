package fr.ign.cogit.parcelFunction;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

import com.opencsv.CSVReader;

import fr.ign.cogit.GTFunctions.Vectors;

public class ParcelState {

//	public static void main(String[] args) throws Exception {
//		File geoFile = new File("/home/ubuntu/boulot/these/result2903/dataGeo/");
//		File batiFile = new File(geoFile, "building.shp");
//		File parcelFile = new File("/tmp/parcelTested.shp");
//		ShapefileDataStore sds = new ShapefileDataStore(parcelFile.toURI().toURL());
//		SimpleFeatureIterator sfc = sds.getFeatureSource().getFeatures().features();
//
//		try {
//			while (sfc.hasNext()) {
//				SimpleFeature sf = sfc.next();
//				System.out.println("sf " + sf.getAttribute("NUMERO"));
//				long startTime2 = System.currentTimeMillis();
//
//				isAlreadyBuilt(batiFile, sf);
//				long endTime2 = System.nanoTime();
//				System.out.println("duration for isAlreadyBuilt : " + (endTime2 - startTime2) * 1000);
//			}
//
//		} catch (Exception problem) {
//			problem.printStackTrace();
//		} finally {
//			sfc.close();
//		}
//		sds.dispose();
//	}

	/**
	 * return false if the parcel mandatory needs a contact with the road to be
	 * urbanized. return true otherwise TODO haven't done it for the zones because I
	 * only found communities that set the same rule regardless of the zone, but
	 * that could be done
	 * 
	 * @param feat     : the parcel
	 * @param rootFile : the rootFile of ArtiScales's project
	 * @return
	 * @throws IOException
	 */
	public static boolean isArt3AllowsIsolatedParcel(SimpleFeature feat, File predicateFile) throws IOException {
		return isArt3AllowsIsolatedParcel(
				((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM")), predicateFile);
	}

	public static boolean isArt3AllowsIsolatedParcel(String insee, File predicateFile) throws IOException {

		if(!predicateFile.exists()) {
			return true;
		}
		
		int nInsee = 0;
		int nArt3 = 0;
		// get rule file
		CSVReader rule = new CSVReader(new FileReader(predicateFile));

		// seek for attribute numbers
		String[] firstLine = rule.readNext();
		for (int i = 0; i < firstLine.length; i++) {
			String s = firstLine[i];
			if (s.equals("insee")) {
				nInsee = i;
			} else if (s.equals("art_3")) {
				nArt3 = i;
			}
		}

		for (String[] line : rule.readAll()) {
			if (insee.equals(line[nInsee])) {
				if (line[nArt3].equals("1")) {
					rule.close();
					return false;
				} else {
					rule.close();
					return true;
				}
			}
		}

		rule.close();
		return true;
	}

	/**
	 * Return true if the parcel is already built 
	 * 
	 * @param feature
	 * @param geoFile
	 * @return
	 * @throws IOException
	 */
	public static boolean isAlreadyBuilt(File batiFile, SimpleFeature feature) throws IOException {
		ShapefileDataStore batiSDS = new ShapefileDataStore(batiFile.toURI().toURL());
		SimpleFeatureCollection batiFeatures = batiSDS.getFeatureSource().getFeatures();
		boolean result = isAlreadyBuilt(batiFeatures, feature);
		batiSDS.dispose();
		return result;
	}

	public static boolean isAlreadyBuilt(SimpleFeatureCollection batiSFC, SimpleFeature feature) throws IOException {
		return isAlreadyBuilt(batiSFC, feature, 0.0);
	}

	/**
	 * overload to select only a selection of buildings
	 * 
	 * @param buildingFile
	 * @param parcel
	 * @param emprise
	 * @return
	 * @throws Exception
	 */
	public static boolean isAlreadyBuilt(File buildingFile, SimpleFeature parcel, Geometry emprise) throws Exception {
		ShapefileDataStore batiSDS = new ShapefileDataStore(buildingFile.toURI().toURL());
		SimpleFeatureCollection batiFeatures = batiSDS.getFeatureSource().getFeatures();
		boolean result = isAlreadyBuilt(Vectors.snapDatas(batiFeatures, emprise), parcel, 0.0);
		batiSDS.dispose();
		return result;

	}

	// public static boolean isAlreadyBuiltStream(SimpleFeatureCollection batiSFC,
	// SimpleFeature feature, double bufferBati) throws IOException {
	// boolean res = false;
	// List<String> result = Arrays.stream(batiSFC.toArray(new SimpleFeature[0]))
	// .forEach(batiFeature -> {
	// if (((Geometry) feature.getDefaultGeometry()).intersects(((Geometry)
	// batiFeature.getDefaultGeometry()).buffer(bufferBati))) {
	// res = true ;
	// }
	// }
	// );
	// }

	public static boolean isAlreadyBuilt(SimpleFeatureCollection batiSFC, SimpleFeature feature, double bufferBati)
			throws IOException {
		boolean isContent = false;
		SimpleFeatureIterator iterator = batiSFC.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature batiFeature = iterator.next();
				if (((Geometry) feature.getDefaultGeometry())
						.intersects(((Geometry) batiFeature.getDefaultGeometry()).buffer(bufferBati))) {
					isContent = true;
					break;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			iterator.close();
		}
		return isContent;
	}

//	public static Double getEvalInParcel(IFeature parcel, File outMup)
//			throws NoSuchAuthorityCodeException, ParseException, FactoryException, IOException, Exception {
//		if (outMup == null) {
//			return 0.0;
//		}
//		return getEvalInParcel(GeOxygeneGeoToolsTypes.convert2SimpleFeature(parcel, CRS.decode("EPSG:2154")), outMup);
//	}

	/**
	 * 
	 * @param parcelIn
	 * @return
	 * @throws ParseException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 * @throws IOException
	 */
	public static Double getEvalInParcel(SimpleFeature parcel, File outMup)
			throws ParseException, NoSuchAuthorityCodeException, FactoryException, IOException {

		ShapefileDataStore cellsSDS = new ShapefileDataStore(outMup.toURI().toURL());
		SimpleFeatureCollection cellsCollection = cellsSDS.getFeatureSource().getFeatures();
		Double result = getEvalInParcel(parcel, cellsCollection);
		cellsSDS.dispose();
		return result;
	}

	public static Double getEvalInParcel(SimpleFeature parcel, SimpleFeatureCollection mupSFC) {

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryCellPropertyName = mupSFC.getSchema().getGeometryDescriptor().getLocalName();

		Filter inter = ff.intersects(ff.property(geometryCellPropertyName), ff.literal(parcel.getDefaultGeometry()));
		SimpleFeatureCollection onlyCells = mupSFC.subCollection(inter);
		Double bestEval = 0.0;

		// put the best cell evaluation into the parcel
		if (onlyCells.size() > 0) {
			SimpleFeatureIterator onlyCellIt = onlyCells.features();
			try {
				while (onlyCellIt.hasNext()) {
					SimpleFeature multiCell = onlyCellIt.next();
					bestEval = Math.max(bestEval, (Double) multiCell.getAttribute("eval"));
				}
			} catch (Exception problem) {
				problem.printStackTrace();
			} finally {
				onlyCellIt.close();
			}
		}

		// si jamais le nom est déjà généré

		// sort collection with evaluation
		// PropertyName pN = ff.property("eval");
		// SortByImpl sbt = new SortByImpl(pN,
		// org.opengis.filter.sort.SortOrder.DESCENDING);
		// SimpleFeatureCollection collectOut = new
		// SortedSimpleFeatureCollection(newParcel, new SortBy[] { sbt });
		//
		// moyenneEval(collectOut);

		return bestEval;
	}

	/**
	 * If we want an evaluation for a parcel that is not intersected by a MUP-City
	 * cell, we will increasly seek for a cell around The seeking is made 5 meters
	 * by 5 meters and the first cell found is chosen The evaluation of this cell is
	 * then sent
	 * 
	 * @param parcel
	 * @param mupSFC
	 * @return
	 */
	public static Double getCloseEvalInParcel(SimpleFeature parcel, SimpleFeatureCollection mupSFC) {

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		String geometryCellPropertyName = mupSFC.getSchema().getGeometryDescriptor().getLocalName();

		Filter inter = ff.intersects(ff.property(geometryCellPropertyName),
				ff.literal(((Geometry) parcel.getDefaultGeometry()).buffer(100.0)));
		SimpleFeatureCollection onlyCells = mupSFC.subCollection(inter);
		Double bestEval = 0.0;

		// put the best cell evaluation into the parcel
		if (onlyCells.size() > 0) {
			double distBuffer = 0.0;
			// we randomly decide that the cell cannot be further than 100 meters
			while (distBuffer < 100) {
				Geometry geometryUp = ((Geometry) parcel.getDefaultGeometry()).buffer(distBuffer);
				SimpleFeatureIterator onlyCellIt = onlyCells.features();
				try {
					while (onlyCellIt.hasNext()) {
						SimpleFeature cell = onlyCellIt.next();
						if (geometryUp.intersects((Geometry) cell.getDefaultGeometry())) {
							return ((Double) cell.getAttribute("eval"));
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					onlyCellIt.close();
				}
				distBuffer = distBuffer + 5;
			}
		}
		return bestEval;
	}

	public static boolean isParcelInCell(SimpleFeature parcelIn, SimpleFeatureCollection cellsCollection)
			throws Exception {

		cellsCollection = Vectors.snapDatas(cellsCollection, (Geometry) parcelIn.getDefaultGeometry());

		// import of the cells of MUP-City outputs
		SimpleFeatureIterator cellsCollectionIt = cellsCollection.features();

		try {
			while (cellsCollectionIt.hasNext()) {
				SimpleFeature cell = cellsCollectionIt.next();
				if (((Geometry) cell.getDefaultGeometry()).intersects(((Geometry) parcelIn.getDefaultGeometry()))) {
					return true;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			cellsCollectionIt.close();
		}
		return false;
	}

	/**
	 * return a single TYPEZONE that a parcels intersect if the parcel intersects
	 * multiple, we select the one that covers the most area
	 * 
	 * @param parcelIn
	 * @param zoningFile
	 * @return
	 * @throws Exception
	 */
	public static String parcelInBigZone(File zoningFile, SimpleFeature parcelIn) throws Exception {
		List<String> bigZones = parcelInBigZone(parcelIn, zoningFile);
		if (bigZones.isEmpty())
			return "null";
		return bigZones.get(0);
	}

//	public static List<String> parcelInBigZone(IFeature parcelIn, File zoningFile) throws Exception {
//		return parcelInBigZone(GeOxygeneGeoToolsTypes.convert2SimpleFeature(parcelIn, CRS.decode("EPSG:2154")), zoningFile);
//	}

	/**
	 * return the TYPEZONEs that a parcels intersect result is sorted by the largest
	 * interdected zone to the lowest
	 * 
	 * @param parcelIn
	 * @param zoningFile
	 * @return the multiple parcel intersected, sorted by area of occupation
	 * @throws Exception
	 */
	public static List<String> parcelInBigZone(SimpleFeature parcelIn, File zoningFile) throws Exception {
		List<String> result = new LinkedList<String>();
		ShapefileDataStore shpDSZone = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection shpDSZoneReduced = Vectors.snapDatas(shpDSZone.getFeatureSource().getFeatures(),
				(Geometry) parcelIn.getDefaultGeometry());
		SimpleFeatureIterator featuresZones = shpDSZoneReduced.features();
		// if there's two zones, we need to sort them by making collection. zis iz évy
		// calculation, but it could worth it
		boolean twoZones = false;
		HashMap<String, Double> repart = new HashMap<String, Double>();

		try {
			zoneLoop: while (featuresZones.hasNext()) {
				SimpleFeature feat = featuresZones.next();
				PrecisionModel precMod = new PrecisionModel(100);
				Geometry featGeometry = GeometryPrecisionReducer.reduce((Geometry) feat.getDefaultGeometry(), precMod);
				Geometry parcelInGeometry = GeometryPrecisionReducer.reduce((Geometry) parcelIn.getDefaultGeometry(),
						precMod);
				if (featGeometry.buffer(0.5).contains(parcelInGeometry)) {
					twoZones = false;
					switch ((String) feat.getAttribute("TYPEZONE")) {
					case "U":
					case "ZC":
						result.add("U");
						result.remove("AU");
						result.remove("NC");
						break zoneLoop;
					case "AU":
					case "TBU":
						result.add("AU");
						result.remove("U");
						result.remove("NC");
						break zoneLoop;
					case "N":
					case "NC":
					case "A":
						result.add("NC");
						result.remove("AU");
						result.remove("U");
						break zoneLoop;
					}
				}
				// maybe the parcel is in between two zones (less optimized) intersection
				else if ((featGeometry).intersects(parcelInGeometry)) {
					twoZones = true;
					double area = Vectors
							.scaledGeometryReductionIntersection(Arrays.asList(featGeometry, parcelInGeometry))
							.getArea();
					switch ((String) feat.getAttribute("TYPEZONE")) {
					case "U":
					case "ZC":
						if (repart.containsKey("U")) {
							repart.put("U", repart.get("U") + area);
						} else {
							repart.put("U", area);
						}
						break;
					case "AU":
					case "TBU":
						if (repart.containsKey("AU")) {
							repart.put("AU", repart.get("AU") + area);
						} else {
							repart.put("AU", area);
						}
						break;
					case "N":
					case "NC":
					case "A":
						if (repart.containsKey("NC")) {
							repart.put("NC", repart.get("NC") + area);
						} else {
							repart.put("NC", area);
						}
						break;
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			featuresZones.close();
		}
		shpDSZone.dispose();

		if (twoZones == true) {
			List<Entry<String, Double>> entryList = new ArrayList<Entry<String, Double>>(repart.entrySet());
			Collections.sort(entryList, new Comparator<Entry<String, Double>>() {
				@Override
				public int compare(Entry<String, Double> obj1, Entry<String, Double> obj2) {
					return obj2.getValue().compareTo(obj1.getValue());
				}
			});

			for (Entry<String, Double> s : entryList) {
				result.add(s.getKey());
			}
		}
		return result;
	}

	/**
	 * return a single typology that a parcels intersect if the parcel intersects
	 * multiple, we select the one that covers the most area
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return
	 * @throws Exception
	 */
	public static String parcelInTypo(File communeFile, SimpleFeature parcelIn) throws Exception {
		return parcelInTypo(parcelIn, communeFile).get(0);
	}

	/**
	 * return the typologies that a parcels intersect
	 * 
	 * @param parcelIn
	 * @param regulFile
	 * @return the multiple parcel intersected, sorted by area of occupation
	 * @throws Exception
	 */
	public static List<String> parcelInTypo(SimpleFeature parcelIn, File communeFile) throws Exception {
		List<String> result = new ArrayList<String>();
		ShapefileDataStore shpDSZone = new ShapefileDataStore(communeFile.toURI().toURL());
		SimpleFeatureCollection shpDSZoneReduced = Vectors.snapDatas(shpDSZone.getFeatureSource().getFeatures(),
				(Geometry) parcelIn.getDefaultGeometry());

		SimpleFeatureIterator featuresZones = shpDSZoneReduced.features();

		// objects for crossed zones
		boolean twoZones = false;
		HashMap<String, Double> repart = new HashMap<String, Double>();

		try {
			zone: while (featuresZones.hasNext()) {
				SimpleFeature feat = featuresZones.next();
				Geometry parcelInGeometry = (Geometry) parcelIn.getDefaultGeometry();
				Geometry featGeometry = (Geometry) feat.getDefaultGeometry();
				// TODO if same typo in two different typo, won't fall into that trap =>
				// create a big zone shapefile instead?
				if (featGeometry.buffer(1).contains(parcelInGeometry)) {
					switch ((String) feat.getAttribute("typo")) {
					case "rural":
						result.add("rural");
						result.remove("periUrbain");
						result.remove("banlieue");
						result.remove("centre");
						break zone;
					case "periUrbain":
						result.add("periUrbain");
						result.remove("rural");
						result.remove("banlieue");
						result.remove("centre");
						break zone;
					case "banlieue":
						result.add("banlieue");
						result.remove("rural");
						result.remove("periUrbain");
						result.remove("centre");
						break zone;
					case "centre":
						result.add("centre");
						result.remove("rural");
						result.remove("periUrbain");
						result.remove("banlieue");
						break zone;
					}
				}
				// maybe the parcel is in between two cities
				else if (featGeometry.intersects(parcelInGeometry)) {
					twoZones = true;
					double area = Vectors
							.scaledGeometryReductionIntersection(Arrays.asList(featGeometry, parcelInGeometry))
							.getArea();
					switch ((String) feat.getAttribute("typo")) {
					case "rural":
						if (repart.containsKey("rural")) {
							repart.put("rural", repart.get("rural") + area);
						} else {
							repart.put("rural", area);
						}
						break;
					case "centre":
						if (repart.containsKey("centre")) {
							repart.put("centre", repart.get("centre") + area);
						} else {
							repart.put("centre", area);
						}
						break;
					case "banlieue":
						if (repart.containsKey("banlieue")) {
							repart.put("banlieue", repart.get("banlieue") + area);
						} else {
							repart.put("banlieue", area);
						}
						break;
					case "periUrbain":
						if (repart.containsKey("periUrbain")) {
							repart.put("periUrbain", repart.get("periUrbain") + area);
						} else {
							repart.put("periUrbain", area);
						}
						break;
					}
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			featuresZones.close();
		}

		if (twoZones == true) {
			List<Entry<String, Double>> entryList = new ArrayList<Entry<String, Double>>(repart.entrySet());
			Collections.sort(entryList, new Comparator<Entry<String, Double>>() {
				@Override
				public int compare(Entry<String, Double> obj1, Entry<String, Double> obj2) {
					return obj2.getValue().compareTo(obj1.getValue());
				}
			});

			for (Entry<String, Double> s : entryList) {
				result.add(s.getKey());
			}
		}

		shpDSZone.dispose();

		if (result.isEmpty()) {
			result.add("null");
		}
		return result;
	}
}
