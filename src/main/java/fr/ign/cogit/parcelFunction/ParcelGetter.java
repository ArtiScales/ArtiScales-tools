package fr.ign.cogit.parcelFunction;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.data.DataUtilities;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import fr.ign.cogit.FeaturePolygonizer;
import fr.ign.cogit.GTFunctions.Vectors;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;
import fr.ign.cogit.geoxygene.feature.FT_FeatureCollection;
import fr.ign.cogit.geoxygene.spatial.coordgeom.DirectPosition;



public class ParcelGetter {
	
	public static void main(String[] args) throws Exception {
		
	}
	
	public static SimpleFeatureCollection getParcelByBigZone(String zone, SimpleFeatureCollection parcelles, File rootFile, File zoningFile)
			throws IOException {
		ShapefileDataStore zonesSDS = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection zonesSFCBig = zonesSDS.getFeatureSource().getFeatures();
		SimpleFeatureCollection zonesSFC = Vectors.cropSFC(zonesSFCBig, parcelles);
		List<String> listZones = new ArrayList<>();
		switch (zone) {
		case "U":
			listZones.add("U");
			listZones.add("ZC");
			break;
		case "AU":
			listZones.add("AU");
			break;
		case "NC":
			listZones.add("A");
			listZones.add("N");
			listZones.add("NC");
			break;
		}

		DefaultFeatureCollection zoneSelected = new DefaultFeatureCollection();
		SimpleFeatureIterator itZonez = zonesSFC.features();
		try {
			while (itZonez.hasNext()) {
				SimpleFeature zones = itZonez.next();
				if (listZones.contains(zones.getAttribute("TYPEZONE"))) {
					zoneSelected.add(zones);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itZonez.close();
		}

		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureIterator it = parcelles.features();
		try {
			while (it.hasNext()) {
				SimpleFeature parcelFeat = it.next();
				SimpleFeatureIterator itZone = zoneSelected.features();
				try {
					while (itZone.hasNext()) {
						SimpleFeature zoneFeat = itZone.next();
						Geometry zoneGeom = (Geometry) zoneFeat.getDefaultGeometry();
						Geometry parcelGeom = (Geometry) parcelFeat.getDefaultGeometry();
						// if (zoneGeom.intersects(parcelGeom)) {
						//
						// result.add(parcelFeat);
						// break;

						if (zoneGeom.contains(parcelGeom)) {
							result.add(parcelFeat);
						}
						// if the intersection is less than 50% of the parcel, we let it to the other
						// (with the hypothesis that there is only 2 features)
						else if (Vectors.scaledGeometryReductionIntersection(Arrays.asList(parcelGeom, zoneGeom)).getArea() > parcelGeom.getArea()
								/ 2) {
							result.add(parcelFeat);
						}
					}

				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itZone.close();
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		zonesSDS.dispose();
		return result.collection();
	}

	public static SimpleFeatureCollection getParcelByTypo(String typo, SimpleFeatureCollection parcelles, File rootFile, File zoningFile)
			throws IOException {

		ShapefileDataStore communitiesSDS = new ShapefileDataStore(zoningFile.toURI().toURL());
		SimpleFeatureCollection communitiesSFCBig = communitiesSDS.getFeatureSource().getFeatures();
		SimpleFeatureCollection communitiesSFC = Vectors.cropSFC(communitiesSFCBig, parcelles);

		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		SimpleFeatureIterator itParcel = parcelles.features();
		try {
			while (itParcel.hasNext()) {
				SimpleFeature parcelFeat = itParcel.next();
				Geometry parcelGeom = (Geometry) parcelFeat.getDefaultGeometry();
				// if tiny parcel, we don't care
				if (parcelGeom.getArea() < 5.0) {
					continue;
				}
				Filter filter = ff.like(ff.property("typo"), typo);
				SimpleFeatureIterator itTypo = communitiesSFC.subCollection(filter).features();
				try {
					while (itTypo.hasNext()) {
						SimpleFeature typoFeat = itTypo.next();
						Geometry typoGeom = (Geometry) typoFeat.getDefaultGeometry();

						if (typoGeom.intersects(parcelGeom)) {
							if (typoGeom.contains(parcelGeom)) {
								result.add(parcelFeat);
								break;
							}
							// if the intersection is less than 50% of the parcel, we let it to the other
							// (with the hypothesis that there is only 2 features)
							// else if (parcelGeom.intersection(typoGeom).getArea() > parcelGeom.getArea() /
							// 2) {

							else if (Vectors.scaledGeometryReductionIntersection(Arrays.asList(typoGeom, parcelGeom))
									.getArea() > (parcelGeom.getArea() / 2)) {
								result.add(parcelFeat);
								break;
							} else {
								break;
							}
						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					itTypo.close();
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			itParcel.close();
		}
		communitiesSDS.dispose();
		return result.collection();
	}

	public static IFeatureCollection<IFeature> getParcelByCode(IFeatureCollection<IFeature> parcelles, List<String> parcelsWanted)
			throws IOException {
		IFeatureCollection<IFeature> result = new FT_FeatureCollection<>();
		for (IFeature parcelle : parcelles) {
			for (String s : parcelsWanted) {
				if (s.equals((String) parcelle.getAttribute("CODE"))) {
					result.add(parcelle);
				}
			}
		}
		return result;
	}

	public static File getParcelByZip(File parcelIn, List<String> vals, File fileOut) throws IOException {
		ShapefileDataStore sds = new ShapefileDataStore(parcelIn.toURI().toURL());
		SimpleFeatureCollection sfc = sds.getFeatureSource().getFeatures();
		SimpleFeatureCollection result = getParcelByZip(sfc, vals);
		sds.dispose();
		return Vectors.exportSFC(result, fileOut);
	}

	public static SimpleFeatureCollection getParcelByZip(SimpleFeatureCollection parcelIn, List<String> vals) throws IOException {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		for (String val : vals) {
			result.addAll(getParcelByZip(parcelIn, val));
		}
		return result.collection();
	}

	public static SimpleFeatureCollection getParcelByZip(SimpleFeatureCollection parcelIn, String val) throws IOException {
		SimpleFeatureIterator it = parcelIn.features();
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		try {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				String insee = ((String) feat.getAttribute("CODE_DEP")).concat(((String) feat.getAttribute("CODE_COM")));
				if (insee.equals(val)) {
					result.add(feat);
				}
			}
		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			it.close();
		}
		return result.collection();
	}
	
	/**
	 * prepare the parcel SimpleFeatureCollection and add necessary attributes and informations for an ArtiScales Simulation overload to run on every cities contained into the
	 * parcel file, simulate a single community and automatically cut all parcels regarding to the zoning file
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param tmpFile
	 *            : Folder where every temporary file is saved
	 * @param zip
	 *            : Community code that must be simulated.
	 * @return the ready to deal with the selection process parcels under a SimpleFeatureCollection format. Also saves it on the tmpFile on a shapeFile format
	 * @throws Exception
	 */
	public static File getParcels(File buildingFile, File zoningFile, File parcelFile, File currentFile, boolean preCutParcels) throws Exception {
		return getParcels(buildingFile, zoningFile, parcelFile, currentFile, new ArrayList<String>());
	}

	/**
	 * prepare the parcel SimpleFeatureCollection and add necessary attributes and informations for an ArtiScales Simulation overload to simulate a single community and
	 * automatically cut all parcels regarding to the zoning file
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param tmpFile
	 *            : Folder where every temporary file is saved
	 * @param zip
	 *            : Community code that must be simulated.
	 * @param preCutParcels
	 *            : if cut all parcels regarding to the zoning file
	 * @return the ready to deal with the selection process parcels under a SimpleFeatureCollection format. Also saves it on the tmpFile on a shapeFile format
	 * @throws Exception
	 */
	public static File getParcels(File buildingFile, File zoningFile, File parcelFile, File tmpFile, String zip, boolean preCutParcels) throws Exception {
		List<String> lZip = new ArrayList<String>();
		lZip.add(zip);
		return getParcels(buildingFile, zoningFile, parcelFile, tmpFile, lZip, null, preCutParcels);
	}

	public static File getParcels(File buildingFile, File zoningFile, File parcelFile, File tmpFile, String zip, File specificParcelsToSimul, boolean preCutParcels)
			throws Exception {
		List<String> lZip = new ArrayList<String>();
		lZip.add(zip);
		return getParcels(buildingFile, zoningFile, parcelFile, tmpFile, lZip, specificParcelsToSimul, preCutParcels);
	}

	/**
	 * prepare the parcel SimpleFeatureCollection and add necessary attributes and informations for an ArtiScales Simulation overload to automatically cut all parcels regarding to
	 * the zoning file
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param tmpFile
	 *            : Folder where every temporary file is saved
	 * @param listZip
	 *            : List of all the communities codes that must be simulated. If empty, we run it on every cities contained into the parcel file
	 * @return the ready to deal with the selection process parcels under a SimpleFeatureCollection format. Also saves it on the tmpFile on a shapeFile format
	 * @throws Exception
	 */
	public static File getParcels(File buildingFile, File zoningFile, File parcelFile, File tmpFile, List<String> listZip) throws Exception {
		return getParcels(buildingFile, zoningFile, parcelFile, tmpFile, listZip, null, true);
	}

	/**
	 * prepare the parcel SimpleFeatureCollection and add necessary attributes and informations for an ArtiScales Simulation
	 * 
	 * @param geoFile
	 *            : the folder containing the geographic data
	 * @param regulFile
	 *            : the folder containing the urban regulation related data
	 * @param tmpFile
	 *            : Folder where every temporary file is saved
	 * @param listZip
	 *            : List of all the communities codes that must be simulated. If empty, we work on every cities contained into the parcel file
	 * @param specificParcelsToSimul
	 *            : shapeFile of specific parcel that will be simulated. If empty, will simulate all parcels
	 * @param preCutParcels
	 *            : if cut all parcels regarding to the zoning file
	 * @return the ready to deal with the selection process parcels under a SimpleFeatureCollection format. Also saves it on the tmpFile on a shapeFile format
	 * @throws Exception
	 */
	public static File getParcels(File buildingFile, File zoningFile, File parcelFile, File tmpFile, List<String> listZip, File specificParcelsToSimul,
			boolean preCutParcels) throws Exception {

		DirectPosition.PRECISION = 3;

//		File result = new File("");
//		for (File f : geoFile.listFiles()) {
//			if (f.toString().contains("parcel.shp")) {
//				result = f;
//			}
//		}
		
		File result = parcelFile;


		ShapefileDataStore parcelSDS = new ShapefileDataStore(result.toURI().toURL());
		SimpleFeatureCollection parcelsSFC = parcelSDS.getFeatureSource().getFeatures();

		ShapefileDataStore shpDSBati = new ShapefileDataStore(buildingFile.toURI().toURL());

		// if we decided to work on a set of parcels
		if (specificParcelsToSimul != null && specificParcelsToSimul.exists()) {
			ShapefileDataStore parcelSpecificSDS = new ShapefileDataStore(specificParcelsToSimul.toURI().toURL());
			parcelsSFC = DataUtilities.collection(parcelSpecificSDS.getFeatureSource().getFeatures());
			parcelSpecificSDS.dispose();
		}
		// if we decided to work on a set of cities
		else if (!listZip.isEmpty()) {
			FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
			DefaultFeatureCollection df = new DefaultFeatureCollection();
			for (String zip : listZip) {

				// could have been nicer, but Filters are some pain in the socks and doesn't
				// work in I factor the code

				// if the zip contains Bensançons's Section parts
				if (zip.length() > 5) {
					// those zip are containing the Section value too
					String codep = zip.substring(0, 2);
					String cocom = zip.substring(2, 5);
					String section = zip.substring(5);

					Filter filterDep = ff.like(ff.property("CODE_DEP"), codep);
					Filter filterCom = ff.like(ff.property("CODE_COM"), cocom);
					Filter filterSection = ff.like(ff.property("SECTION"), section);
					df.addAll(parcelsSFC.subCollection(filterDep).subCollection(filterCom).subCollection(filterSection));
				} else {
					String codep = zip.substring(0, 2);
					String cocom = zip.substring(2, 5);
					Filter filterDep = ff.like(ff.property("CODE_DEP"), codep);
					Filter filterCom = ff.like(ff.property("CODE_COM"), cocom);
					df.addAll(parcelsSFC.subCollection(filterDep).subCollection(filterCom));
				}
			}
			parcelsSFC = df.collection();
		}

		// if we cut all the parcel regarding to the zoning code
		if (preCutParcels) {
			File tmpParcel = Vectors.exportSFC(parcelsSFC, new File(tmpFile, "tmpParcel.shp"));
			File[] polyFiles = { tmpParcel, zoningFile };
			List<Polygon> polygons = FeaturePolygonizer.getPolygons(polyFiles);

			// register to precise every parcel that are in the output
			List<String> codeParcelsTot = new ArrayList<String>();

			// auto parcel feature builder
			SimpleFeatureBuilder sfSimpleBuilder = ParcelSchema.getSimpleParcelSFBuilder();

			DefaultFeatureCollection write = new DefaultFeatureCollection();

			// for every made up polygons out of zoning and parcels
			for (Geometry poly : polygons) {
				// for every parcels around the polygon

				SimpleFeatureCollection snaped = Vectors.snapDatas(parcelsSFC, poly.getBoundary());
				SimpleFeatureIterator parcelIt = snaped.features();
				try {
					while (parcelIt.hasNext()) {
						SimpleFeature feat = parcelIt.next();
						// if the polygon part was between that parcel, we add its attribute
						if (((Geometry) feat.getDefaultGeometry()).buffer(1).contains(poly)) {
							sfSimpleBuilder.set("the_geom", GeometryPrecisionReducer.reduce(poly, new PrecisionModel(100)));
							String code = ParcelAttribute.makeParcelCode(feat);
							sfSimpleBuilder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
							sfSimpleBuilder.set("CODE_COM", feat.getAttribute("CODE_COM"));
							sfSimpleBuilder.set("COM_ABS", feat.getAttribute("COM_ABS"));
							sfSimpleBuilder.set("SECTION", feat.getAttribute("SECTION"));
							String num = (String) feat.getAttribute("NUMERO");
							// if a part has already been added

							if (codeParcelsTot.contains(code)) {
								while (true) {
									num = num + "bis";
									code = code + "bis";
									sfSimpleBuilder.set("NUMERO", num);
									if (!codeParcelsTot.contains(code)) {
										codeParcelsTot.add(code);
										break;
									}
								}
							} else {
								sfSimpleBuilder.set("NUMERO", feat.getAttribute("NUMERO"));
								codeParcelsTot.add(code);
							}
							sfSimpleBuilder.set("CODE", code);
							write.add(sfSimpleBuilder.buildFeature(null));

							// this could be nicer but it doesn't work
							// for (int i = 0; i < codeParcelsTot.size(); i++) {
							// if (codeParcelsTot.get(i).substring(0, 13).equals(code)) {
							// num = num + "bis";
							// code = code + "bis";
							// }
							// }
							// sfSimpleBuilder.set("NUMERO", num);
							// sfSimpleBuilder.set("CODE", code);
							// codeParcelsTot.add(code);
							// write.add(sfSimpleBuilder.buildFeature(null));

						}
					}
				} catch (Exception problem) {
					problem.printStackTrace();
				} finally {
					parcelIt.close();
				}
			}
			parcelsSFC = write.collection();
			Vectors.exportSFC(parcelsSFC, new File("/tmp/dqsdf.shp"));
		}
		// under the carpet
		// ReferencedEnvelope carpet = parcelsSFC.getBounds();
		// Coordinate[] coord = { new Coordinate(carpet.getMaxX(), carpet.getMaxY()), new Coordinate(carpet.getMaxX(), carpet.getMinY()),
		// new Coordinate(carpet.getMinX(), carpet.getMinY()), new Coordinate(carpet.getMinX(), carpet.getMaxY()),
		// new Coordinate(carpet.getMaxX(), carpet.getMaxY()) };
		//
		// GeometryFactory gf = new GeometryFactory();
		// Polygon bbox = gf.createPolygon(coord);
		// SimpleFeatureCollection batiSFC = Vectors.snapDatas(shpDSBati.getFeatureSource().getFeatures(), bbox);
		SimpleFeatureCollection batiSFC = shpDSBati.getFeatureSource().getFeatures();

		// SimpleFeatureCollection batiSFC =
		// Vectors.snapDatas(shpDSBati.getFeatureSource().getFeatures(),
		// Vectors.unionSFC(parcels));

		SimpleFeatureBuilder finalParcelBuilder = ParcelSchema.getParcelSFBuilder();

		DefaultFeatureCollection newParcel = new DefaultFeatureCollection();

		// int tot = parcels.size();
		SimpleFeatureIterator parcelIt = parcelsSFC.features();
		try {
			parc: while (parcelIt.hasNext()) {
				SimpleFeature feat = parcelIt.next();
				Geometry geom = (Geometry) feat.getDefaultGeometry();
				if (geom.getArea() > 5.0) {
					// put the best cell evaluation into the parcel
					// say if the parcel intersects a particular zoning type
					boolean u = false;
					boolean au = false;
					boolean nc = false;

					for (String s : ParcelState.parcelInBigZone(feat, zoningFile)) {
						if (s.equals("AU")) {
							au = true;
						} else if (s.equals("U")) {
							u = true;
						} else if (s.equals("NC")) {
							nc = true;
						} else {
							// if the parcel is outside of the zoning file, we don't keep it
							continue parc;
						}
					}

					finalParcelBuilder.set("the_geom", geom);
					finalParcelBuilder.set("CODE", ParcelAttribute.makeParcelCode(feat));
					finalParcelBuilder.set("CODE_DEP", feat.getAttribute("CODE_DEP"));
					finalParcelBuilder.set("CODE_COM", feat.getAttribute("CODE_COM"));
					finalParcelBuilder.set("COM_ABS", feat.getAttribute("COM_ABS"));
					finalParcelBuilder.set("SECTION", feat.getAttribute("SECTION"));
					finalParcelBuilder.set("NUMERO", feat.getAttribute("NUMERO"));
					finalParcelBuilder.set("INSEE", ((String) feat.getAttribute("CODE_DEP")) + ((String) feat.getAttribute("CODE_COM")));
					finalParcelBuilder.set("eval", 0);
					finalParcelBuilder.set("DoWeSimul", false);
					finalParcelBuilder.set("IsBuild", ParcelState.isAlreadyBuilt( batiSFC,feat));
					finalParcelBuilder.set("U", u);
					finalParcelBuilder.set("AU", au);
					finalParcelBuilder.set("NC", nc);

					newParcel.add(finalParcelBuilder.buildFeature(null));
				}
			}

		} catch (Exception problem) {
			problem.printStackTrace();
		} finally {
			parcelIt.close();
		}

		parcelSDS.dispose();
		shpDSBati.dispose();

		return Vectors.exportSFC(newParcel.collection(), new File(tmpFile, "parcelProcessed.shp"));
	}


}
