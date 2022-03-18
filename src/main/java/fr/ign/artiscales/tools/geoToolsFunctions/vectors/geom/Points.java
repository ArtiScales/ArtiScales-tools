package fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class Points {
    public static Coordinate project(Coordinate p, LineString l) {
        List<Pair<Coordinate, Double>> list = new ArrayList<>();
        for (int i = 0; i < l.getNumPoints() - 1; i++) {
            LineSegment segment = new LineSegment(l.getCoordinateN(i), l.getCoordinateN(i + 1));
            Coordinate proj = segment.closestPoint(p);
            list.add(new ImmutablePair<>(proj, proj.distance(p)));
        }
        return Objects.requireNonNull(list.stream().min(Comparator.comparing(Pair::getRight)).orElse(null)).getLeft();
    }

    /**
     * Generate random points contained in a polygon
     *
     * @param inputPolygon input polygon
     * @param nbPoints     of generated points
     * @return a point inside the wanted polygon
     */
    public static List<Point> randomPointsInPolygon(Polygon inputPolygon, int nbPoints) {
        double xmin = Double.POSITIVE_INFINITY, xmax = Double.NEGATIVE_INFINITY, ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
        for (Coordinate c : inputPolygon.getEnvelope().getCoordinates()) {
            if (c.x < xmin)
                xmin = c.x;
            if (c.y < ymin)
                ymin = c.y;
            if (c.x > xmax)
                xmax = c.x;
            if (c.y > ymax)
                ymax = c.y;
        }
        GeometryFactory gf = inputPolygon.getFactory();
        List<Point> lP = new ArrayList<>();
        for (int i = 0; i < nbPoints; i++)
            lP.add(randomPointInPolygon(inputPolygon, gf, xmin, xmax, ymin, ymax));
        return lP;
    }

    /**
     * Generate a random point contained in a polygon
     *
     * @param inputPolygon input polygon
     * @return a point inside the wanted polygon
     */
    public static Point randomPointInPolygon(Polygon inputPolygon) {
        double xmin = Double.POSITIVE_INFINITY, xmax = Double.NEGATIVE_INFINITY, ymin = Double.POSITIVE_INFINITY, ymax = Double.NEGATIVE_INFINITY;
        for (Coordinate c : inputPolygon.getEnvelope().getCoordinates()) {
            if (c.x < xmin)
                xmin = c.x;
            if (c.y < ymin)
                ymin = c.y;
            if (c.x > xmax)
                xmax = c.x;
            if (c.y > ymax)
                ymax = c.y;
        }
        GeometryFactory gf = inputPolygon.getFactory();
        return randomPointInPolygon(inputPolygon, gf, xmin, xmax, ymin, ymax);
    }

    private static Point randomPointInPolygon(Polygon inputPolygon, GeometryFactory gf, double xmin, double xmax, double ymin, double ymax) {
        Point p;
        do
            p = gf.createPoint(new Coordinate(new Random().nextDouble() * (xmax - xmin) + xmin, new Random().nextDouble() * (ymax - ymin) + ymin));
        while (!inputPolygon.contains(p));
        return p;
    }
}
