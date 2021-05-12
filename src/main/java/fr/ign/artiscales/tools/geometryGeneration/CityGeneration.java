package fr.ign.artiscales.tools.geometryGeneration;

import fr.ign.artiscales.tools.FeaturePolygonizer;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geopackages;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.data.DataStore;
import org.geotools.data.collection.SpatialIndexFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

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
        return CollecMgmt.exportSFC(block, new File(outFolder, "block.shp"));
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
        CollecMgmt.exportSFC(createUrbanBlock(Objects.requireNonNull(parcelDS).getFeatureSource(parcelDS.getTypeNames()[0]).getFeatures()), result);
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
    public static SimpleFeatureCollection createUrbanBlock(List<SimpleFeature> parcel) throws IOException {
        DefaultFeatureCollection df = new DefaultFeatureCollection();
        df.addAll(parcel);
        return createUrbanBlock(df.collection());
    }

    /**
     * Generate urban block out of a parcel plan. Urban block can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
     * Generate a 1.1m buffer and then a -1.1m buffer (this length allow to fill gaps superior to 2.2m, corresponding to the minimal width of a road). It may alter geometries but it fills holes.
     *
     * @param parcel input parcel
     * @return a {@link SimpleFeatureCollection} of urban block
     * @throws IOException
     */
    public static SimpleFeatureCollection createUrbanBlock(SimpleFeatureCollection parcel) throws IOException {
        return createUrbanBlock(parcel, true);
    }

    /**
     * Generate urban block out of a parcel plan. Urban block can be viewed as a block but must have a discontinuity (i.e. road or public space) between them.
     *
     * @param parcel   input parcel
     * @param doBuffer do we generate a 1.1m buffer and then a -1.1m buffer (this length allow to fill gaps superior to 2.2m, corresponding to the minimal width of a road). It may alter geometries but it fills holes.
     * @return a {@link SimpleFeatureCollection} of urban block
     * @throws IOException
     */
    public static SimpleFeatureCollection createUrbanBlock(SimpleFeatureCollection parcel, boolean doBuffer) throws IOException {
        Geometry bigGeom = Geom.unionSFC(parcel);
        if (doBuffer)
            bigGeom = bigGeom.buffer(1.1).buffer(-1.1);
        return createUrbanBlock(bigGeom);
    }

    public static SimpleFeatureCollection createUrbanBlock(Geometry bigGeom) throws IOException {
        DefaultFeatureCollection df = new DefaultFeatureCollection();
        SimpleFeatureBuilder sfBuilder = Schemas.getBasicSchemaID("block");
        AtomicInteger indexGen = new AtomicInteger();
        IntStream.range(0, bigGeom.getNumGeometries()).forEach(x -> {
                    int index = indexGen.getAndIncrement();
                    sfBuilder.add(bigGeom.getGeometryN(index));
                    Object[] obj = {index};
                    df.add(sfBuilder.buildFeature(Attribute.makeUniqueId(), obj));
                }
        );
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
