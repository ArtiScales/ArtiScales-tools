package fr.ign.artiscales.tools.geometryGeneration;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

import java.util.ArrayList;
import java.util.List;

public class VoronoiPolygon {
    public static List<Polygon> makeVoronoiDiagram(SimpleFeatureCollection collec) {
        List<Point> lG = new ArrayList<>();
        try (SimpleFeatureIterator it = collec.features()) {
            while (it.hasNext())
                lG.add((Point) it.next().getDefaultGeometry());
        }
        return makeVoronoiDiagram(lG);
    }

    public static List<Polygon> makeVoronoiDiagram(List<Point> lG) {
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 2154);
        VoronoiDiagramBuilder voronoiDiagram = new VoronoiDiagramBuilder();
        voronoiDiagram.setSites(gf.createMultiPoint(lG.toArray(Point[]::new)));
        Geometry polygonCollection = voronoiDiagram.getDiagram(gf);
        List<Polygon> producedPolygons = new ArrayList<>();
        for (int i = 0; i < polygonCollection.getNumGeometries(); i++)
            producedPolygons.add((Polygon) polygonCollection.getGeometryN(i));
        return producedPolygons;
    }
}
