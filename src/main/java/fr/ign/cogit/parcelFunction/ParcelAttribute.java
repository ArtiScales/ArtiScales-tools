package fr.ign.cogit.parcelFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

public class ParcelAttribute {
	public static String makeParcelCode(SimpleFeature feat) {
		return ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM")) + ((String) feat.getAttribute("COM_ABS"))
				+ ((String) feat.getAttribute("SECTION")) + ((String) feat.getAttribute("NUMERO"));
	}

	public static String makeINSEECode(SimpleFeature feat) {
		return ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM"));
	}

	/**
	 * get the insee number from a Simplefeature (that is most of the time, a parcel or building)
	 * 
	 * @param cities
	 * @param parcel
	 * @return
	 */
	public static String getInseeFromParcel(SimpleFeatureCollection cities, SimpleFeature parcel) {
		return getCommunityStuffFromParcel(cities, parcel, "DEPCOM");
	}

	public static String getArmatureFromParcel(SimpleFeatureCollection cities, SimpleFeature parcel) {
		return getCommunityStuffFromParcel(cities, parcel, "armature");
	}

	public static String getCommunityStuffFromParcel(SimpleFeatureCollection cities, SimpleFeature parcel, String code) {

		SimpleFeature city = null;
		SimpleFeatureIterator citIt = cities.features();
		String result = "";
		try {
			while (citIt.hasNext()) {
				SimpleFeature cit = citIt.next();
				if (((Geometry) cit.getDefaultGeometry()).contains((Geometry) parcel.getDefaultGeometry())) {
					city = cit;
					break;
				}
				// if the parcel is in between two cities, we randomly add the first met
				else if (((Geometry) cit.getDefaultGeometry()).intersects((Geometry) parcel.getDefaultGeometry())) {
					city = cit;
					break;
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			citIt.close();
		}

		String attribute = (String) city.getAttribute(code);
		if (attribute != null && !attribute.isEmpty()) {
			result = attribute;
		}
		return result;
	}

	// public static List<String> getCodeParcelsStream(SimpleFeatureCollection parcels) {
	//
	// List<String> result = Arrays.stream(parcels.toArray(new SimpleFeature[0]))
	// // .map(feat -> ((String) feat.getAttribute("CODE")))
	// .forEach( feat -> {
	// code = ((String) feat.getAttribute("CODE"));
	// if (code == null || code.isEmpty()) {
	// code = makeParcelCode(feat);
	// }
	//// -> code;
	// })
	// .collect(Collectors.toList());
	//
	// return result;
	// }

	/**
	 * get the parcel codes (Attribute CODE of the given SimpleFeatureCollection)
	 * 
	 * @param parcels
	 * @return
	 */
	public static List<String> getCodeParcels(SimpleFeatureCollection parcels) {

		List<String> result = new ArrayList<String>();
		SimpleFeatureIterator parcelIt = parcels.features();
		try {
			while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				String code = ((String) feat.getAttribute("CODE"));
				if (code != null && !code.isEmpty()) {
					result.add(code);
				} else {
					result.add(makeParcelCode(feat));
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}
		return result;
	}

	/**
	 * get a list of all the INSEE numbers of the parcels in the collection
	 * 
	 * @overload for automatic INSEE name field in the French case
	 * @param parcels
	 *            : a collection of parcels TODO to test with the stream
	 * @return
	 */
	public static List<String> getCityCodeFromParcels(SimpleFeatureCollection parcels) {
		return getCityCodeFromParcels(parcels, "INSEE");
	}

	/**
	 * get a list of all the INSEE numbers of the parcels in the collection
	 * 
	 * @param parcels
	 *            : a collection of parcels
	 * @param String
	 *            containing the name of the field containing the city's code TODO to test with the stream
	 * @return
	 */
	public static List<String> getCityCodeFromParcels(SimpleFeatureCollection parcels, String cityField) {
		List<String> result = new ArrayList<String>();
		Arrays.stream(parcels.toArray(new SimpleFeature[0])).forEach(feat -> {
			String code = ((String) feat.getAttribute(cityField));
			if (code != null && !code.isEmpty()) {
				result.add(code);
			} else {
				String c = makeINSEECode(feat);
				if (!result.contains(c)) {
					result.add(makeINSEECode(feat));
				}
			}
		});

		// List<String> result = new ArrayList<String>();
		// SimpleFeatureIterator parcelIt = parcels.features();
		// try {
		// while (parcelIt.hasNext()) {
		// SimpleFeature feat = parcelIt.next();
		// String code = ((String) feat.getAttribute("INSEE"));
		// if (code != null && !code.isEmpty()) {
		// result.add(code);
		// } else {
		// String c = makeINSEECode(feat);
		// if (!result.contains(c)) {
		// result.add(makeINSEECode(feat));
		// }
		// }
		//
		// }
		// } catch (Exception problem) {
		// problem.printStackTrace();
		// } finally {
		// parcelIt.close();
		// }
		return result;
	}

	public static String normalizeNameBigZone(String nameZone) throws Exception {
		System.out.println(nameZone);
		switch (nameZone) {
		case "U":
		case "ZC":
		case "C":
			return "U";
		case "AU":
			return "AU";
		case "N":
		case "A":
		case "NC":
		case "ZNC":
			return "NC";
		}
		throw new Exception("unknown big zone name");
	}
}
