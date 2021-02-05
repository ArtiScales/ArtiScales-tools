package fr.ign.artiscales.tools.carto;

import java.util.Arrays;
import java.util.HashMap;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecTransform;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

public class MergeByAttribute {

//	public static void main(String[] args) throws IOException {
//		DataStore ds = Geopackages.getDataStore(new File("/home/ubuntu/Documents/DensificationStudy-out/parcel.gpkg"));
//		CollecMgmt.exportSFC(mergeByAttribute(Objects.requireNonNull(ds).getFeatureSource(ds.getTypeNames()[0]).getFeatures(), "CODE_COM"), new File("/tmp/test"));
//	}

	public static SimpleFeatureCollection mergeByAttribute(SimpleFeatureCollection in, String attribute) {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		if (!CollecMgmt.isCollecContainsAttribute(in, attribute)) {
			System.out.println("mergeByAttribute:  no " + attribute + " found");
			return null;
		}
		HashMap<String, SimpleFeatureCollection> merges = new HashMap<>();
		CollecMgmt.getEachUniqueFieldFromSFC(in, attribute).forEach(uniqueVal -> {
			DefaultFeatureCollection list = new DefaultFeatureCollection();
			Arrays.stream(in.toArray(new SimpleFeature[0])).forEach(sf -> {
				if (sf.getAttribute(attribute).equals(uniqueVal))
					list.add(sf);
			});
			merges.put(uniqueVal, list);
		});
		for (String val : merges.keySet()) 
			result.add(CollecTransform.unionSFC(merges.get(val), attribute, val));
		return result;
	}
}
