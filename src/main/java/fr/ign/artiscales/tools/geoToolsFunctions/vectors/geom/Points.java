package fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;

public class Points {
	public static Coordinate project(Coordinate p, LineString l) {
		List<Pair<Coordinate, Double>> list = new ArrayList<>();
		for (int i = 0; i < l.getNumPoints() - 1; i++) {
			LineSegment segment = new LineSegment(l.getCoordinateN(i), l.getCoordinateN(i + 1));
			Coordinate proj = segment.closestPoint(p);
			list.add(new ImmutablePair<>(proj, proj.distance(p)));
		}
		return list.stream().min(Comparator.comparing(Pair::getRight)).get().getLeft();
	}
}
