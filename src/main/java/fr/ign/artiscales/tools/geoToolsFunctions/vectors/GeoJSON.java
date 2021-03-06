package fr.ign.artiscales.tools.geoToolsFunctions.vectors;

import fr.ign.artiscales.tools.io.Json;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.geojson.GeoJSONDataStoreFactory;
import org.geotools.util.URLs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GeoJSON extends Json {
    public static DataStore getGeoJSONDataStore(File geojsonFile) throws IOException {
        Map<String, Object> params = new HashMap<>();
        params.put(GeoJSONDataStoreFactory.URL_PARAM.key, URLs.fileToUrl(geojsonFile));
        return DataStoreFinder.getDataStore(params);
    }
    /*
     * ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures()
     */
}
