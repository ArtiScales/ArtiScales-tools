package fr.ign.artiscales.tools.carto;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geopackages;

public class MergeByAttribute {

	public static void main(String[] args) throws IOException {
		DataStore ds = Geopackages.getDataStore(new File("/home/ubuntu/Documents/DensificationStudy-out/parcel.gpkg"));
		Collec.exportSFC(mergeByAttribute(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), "CODE_COM"), new File("/tmp/test"));
	}

	public static SimpleFeatureCollection mergeByAttribute(SimpleFeatureCollection in, String attribute) {
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		if (!Collec.isCollecContainsAttribute(in, attribute)) {
			System.out.println("mergeByAttribute:  no " + attribute + " found");
			return null;
		}
		HashMap<String, SimpleFeatureCollection> merges = new HashMap<String, SimpleFeatureCollection>();
		for (String uniqueVal : Collec.getEachUniqueFieldFromSFC(in, attribute)) {
			DefaultFeatureCollection list = new DefaultFeatureCollection();
			Arrays.stream(in.toArray(new SimpleFeature[0])).forEach(sf -> {
				if (sf.getAttribute(attribute).equals(uniqueVal))
					list.add(sf);
			}) ;
			merges.put(uniqueVal, list);
		}
		for (String val : merges.keySet()) 
			result.add(Collec.unionSFC(merges.get(val), attribute, val));
		return result;
	}
}
