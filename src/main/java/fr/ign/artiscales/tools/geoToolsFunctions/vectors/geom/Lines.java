package fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.operation.linemerge.LineMerger;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;

public class Lines {
	public static MultiLineString generateLineStringFromPolygon(Geometry geom) {
		Polygon poly = Polygons.getPolygon(geom);
		List<Geometry> lines = new ArrayList<Geometry>();
		lines.add(geom.getFactory().createMultiLineString(new LineString[] { poly.getExteriorRing() }));
		for (int i = 0; i < poly.getNumInteriorRing(); i++) {
			LineString interiorLines = poly.getInteriorRingN(i);
			// avoid silvers
			if (interiorLines.getLength() > 100.0)
				lines.add(geom.getFactory().createMultiLineString(new LineString[] { interiorLines }));
		}
		return (MultiLineString) Geom.unionGeom(lines);
	}
	
	public static MultiLineString getMultiLineString(Geometry geom) {
	    List<LineString> list = getLineStrings(geom);
	    return geom.getFactory().createMultiLineString(list.toArray(new LineString[list.size()]));
	}

	public static MultiLineString getListLineStringAsMultiLS(List<LineString> list, GeometryFactory fact) {
		return fact.createMultiLineString(list.toArray(new LineString[list.size()]));
	}

	public static List<LineString> fromMultiToLineString(MultiLineString ml) {
		List<LineString> lL = new ArrayList<LineString>();
		for (int i = 0; i < ml.getNumGeometries(); i++)
			lL.add((LineString) ml.getGeometryN(i));
		return lL;
	}

	public static List<LineString> getSegments(LineString l) {
		List<LineString> result = new ArrayList<>();
		for (int i = 0; i < l.getNumPoints() - 1; i++)
			result.add(l.getFactory().createLineString(new Coordinate[] { l.getCoordinateN(i), l.getCoordinateN(i + 1) }));
		return result;
	}

	/**
	 * Get a list of {@link LineString} from a various kind of Geometry inputs
	 * 
	 * @param geom
	 * @return
	 */
	public static List<LineString> getLineStrings(Geometry geom) {
		if (geom instanceof LineString)
			return Arrays.asList((LineString) geom);
		else if (geom instanceof MultiLineString)
			return fromMultiToLineString((MultiLineString) geom);
		else if (geom instanceof Polygon)
			return fromMultiToLineString(generateLineStringFromPolygon(geom));
		else if (geom instanceof MultiPolygon)
			return Polygons.getPolygons(geom).stream().map(g -> fromMultiToLineString(generateLineStringFromPolygon(g))).collect(ArrayList::new,
					ArrayList::addAll, ArrayList::addAll);
		else if (geom instanceof GeometryCollection) {
			List<LineString> result = new ArrayList<>();
			for (int i = 0; i < geom.getNumGeometries(); i++)
				result.addAll(getLineStrings(geom.getGeometryN(i)));
			return result;
		} else
			System.out.println("getLineString(): Geometry class unknown - " + geom.getGeometryType());
		return null;
	}

	public static Pair<LineString, LineString> splitLine(LineString line, double s) {
		LengthIndexedLine lil = new LengthIndexedLine(line);
		return new ImmutablePair<LineString, LineString>((LineString) lil.extractLine(0, s), (LineString) lil.extractLine(s, line.getLength()));
	}

	public static Pair<LineString, LineString> splitLine(LineString line, Coordinate c) {
		LengthIndexedLine lil = new LengthIndexedLine(line);
		return splitLine(line, lil.indexOf(c));
	}

	static boolean getRayLineSegmentIntersects(Coordinate o, Coordinate d, Coordinate a, Coordinate b) {
		Vector2D ortho = Vector2D.create(-d.y, d.x);
		Vector2D aToO = Vector2D.create(a, o);
		Vector2D aToB = Vector2D.create(a, b);
		double denom = aToB.dot(ortho);
		// if (denom < 0) {
		// ortho = Vector2D.create(d.y, -d.x);
		// denom = aToB.dot(ortho);
		// }
		// System.out.println("denom = " + denom);
		// Here would be a good time to see if denom is zero in which case the line segment and the ray are parallel.
		if (denom == 0)
			return false; // TODO : add tolerance?
		double length = aToB.getX() * aToO.getY() - aToO.getX() * aToB.getY();
		double t1 = length / denom;
		double t2 = aToO.dot(ortho) / denom;
		// System.out.println("t1 = " + t1 + " t2 = " + t2);
		return t2 >= 0 && t2 <= 1 && t1 >= 0;
	}

	static Coordinate getRayLineSegmentIntersection(Coordinate o, Coordinate d, Coordinate a, Coordinate b) {
		Vector2D ortho = Vector2D.create(-d.y, d.x);
		Vector2D aToO = Vector2D.create(a, o);
		Vector2D aToB = Vector2D.create(a, b);
		double denom = aToB.dot(ortho);
		System.out.println("DENOM= " + denom);
		// if (denom < 0) {
		// ortho = Vector2D.create(d.y, -d.x);
		// denom = aToB.dot(ortho);
		// }
		// Here would be a good time to see if denom is zero in which case the line segment and the ray are parallel.
		if (denom == 0)
			return null; // TODO : add tolerance?
		double length = aToB.getX() * aToO.getY() - aToO.getX() * aToB.getY();
		double t1 = length / denom;
		double t2 = aToO.dot(ortho) / denom;
		System.out.println("t1= " + t1);
		if (t2 >= 0 && t2 <= 1 && t1 >= 0)
			return new Coordinate(a.getX() + t2 * aToB.getX(), a.getY() + t2 * aToB.getY());
		return null;
	}

	public static boolean getRayLineSegmentIntersects(Coordinate o, Coordinate d, LineString line) {
		Coordinate a = line.getCoordinateN(0);
		Coordinate b = line.getCoordinateN(line.getNumPoints() - 1);
		return getRayLineSegmentIntersects(o, d, a, b);
	}

	public static Coordinate getRayLineSegmentIntersection(Coordinate o, Coordinate d, LineString line) {
		Coordinate a = line.getCoordinateN(0);
		Coordinate b = line.getCoordinateN(line.getNumPoints() - 1);
		return getRayLineSegmentIntersection(o, d, a, b);
	}

	public static LineString union(List<LineString> list) {
		if (list.isEmpty())
			return null;
		LineMerger merger = new LineMerger();
		list.forEach(l -> merger.add(l));
		return (LineString) merger.getMergedLineStrings().iterator().next();// FIXME we assume a lot here
	}
}
