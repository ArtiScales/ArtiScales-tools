package fr.ign.artiscales.tools.geometryGeneration;

import fr.ign.artiscales.tools.FeaturePolygonizer;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geopackages;
import org.geotools.data.DataStore;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class to generate shapefiles related to community shape.
 *
 * @author Maxime Colomb
 */
public class CityGeneration {

//	public static void main(String[] args) throws NoSuchAuthorityCodeException, IOException, FactoryException {
//	}

    /**
     * Generate urban block shapefile out of a parcel plan. Urban block can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
     *
     * @param parcelFile input parcel
     * @param outFolder  folder where goes the generated urban block
     * @return a collection of urban block
     * @throws IOException
     */
    public static File createUrbanBlockShp(File parcelFile, File outFolder) throws IOException {
        File result = new File(outFolder, "block.shp");
        if (result.exists()) {
            System.out.println("createUrbanBlock(): block already exists");
            return result;
        }
        ShapefileDataStore parcelSDS = new ShapefileDataStore(parcelFile.toURI().toURL());
        SimpleFeatureCollection block = createUrbanBlock(parcelSDS.getFeatureSource().getFeatures());
        parcelSDS.dispose();
        return Collec.exportSFC(block, new File(outFolder, "block.shp"));
    }

    /**
     * Generate urban block shapefile out of a parcel plan. Urban block can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
     *
     * @param parcelFile input parcel
     * @param outFolder  folder where goes the generated urban block
     * @return a collection of urban block
     * @throws IOException
     */
    public static File createUrbanBlock(File parcelFile, File outFolder) throws IOException {
        File result = new File(outFolder, "block.gpkg");
        if (result.exists()) {
            System.out.println("createUrbanblock(): block already exists");
            return result;
        }
        DataStore parcelDS = Geopackages.getDataStore(parcelFile);
        Collec.exportSFC(createUrbanBlock(Objects.requireNonNull(parcelDS).getFeatureSource(parcelDS.getTypeNames()[0]).getFeatures()), result);
        parcelDS.dispose();
        return result;
    }

    /**
     * Generate urban block out of a parcel plan. Urban block can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
     *
     * @param parcel input parcel
     * @return a {@link SimpleFeatureCollection} of urban block
     * @throws IOException
     */
    public static SimpleFeatureCollection createUrbanBlock(SimpleFeatureCollection parcel) throws IOException {
//		Geometry bigGeom = Geom.unionSFC(parcel).buffer(1).buffer(-1);
        Geometry bigGeom = Geom.unionSFC(parcel);
        DefaultFeatureCollection df = new DefaultFeatureCollection();
        SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaID("block");

        AtomicInteger indexGen = new AtomicInteger();
        IntStream.range(0, bigGeom.getNumGeometries())
                .forEach(x -> {
                            int index = indexGen.getAndIncrement();
                            sfBuilder.add(bigGeom.getGeometryN(index));
                            Object[] obj = {index};
                            df.add(sfBuilder.buildFeature(Attribute.makeUniqueId(), obj));
                        }
                );
//		for (int i = 0; i < bigGeom.getNumGeometries(); i++) {
//			sfBuilder.add(bigGeom.getGeometryN(i));
//			Object[] obj = { i };
//			df.add(sfBuilder.buildFeature(String.valueOf(count++), obj));
//		}
        return new SpatialIndexFeatureCollection(df.collection());
    }

    /**
     * Get the border of a studied zone. Buffers have fixed values and could be parametrized.
     *
     * @param in input {@link SimpleFeatureCollection}
     * @return the border without the inside geometry
     */
    public static Geometry createBufferBorder(SimpleFeatureCollection in) {
        Geometry hull = Geom.unionSFC(in).buffer(20).buffer(-20);
        List<Geometry> list = Arrays.asList(hull, hull.buffer(50));
        return Geom.unionGeom(FeaturePolygonizer.getPolygons(list).stream().filter(x -> !hull.buffer(1).contains(x)).collect(Collectors.toList()));
    }
}
