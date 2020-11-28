package fr.ign.artiscales.tools.carto;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.util.List;

public class CountPointInPolygon {

	public static SimpleFeatureCollection countPointInPolygon(SimpleFeatureCollection pointsCollec, SimpleFeatureCollection polygonsCollec) {
		return countPointInPolygon(pointsCollec, polygonsCollec, false, null, null);
	}

	public static SimpleFeatureCollection countPointInPolygon(SimpleFeatureCollection pointsCollec, SimpleFeatureCollection polygonsCollec,
			boolean keepAttributes, List<String> attrsToStat, List<StatisticOperation> statsToDo) {
		// enrich the output wanted schema with wanted stats about attributes
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		SimpleFeatureType schemaOut = polygonsCollec.getSchema();
		String geomName = schemaOut.getGeometryDescriptor().getLocalName();
		sfTypeBuilder.add(geomName, MultiPolygon.class);
		sfTypeBuilder.setName(schemaOut.getName());
		sfTypeBuilder.setCRS(schemaOut.getCoordinateReferenceSystem());
		sfTypeBuilder.setDefaultGeometry(geomName);
		sfTypeBuilder.add("count", Integer.class);
		// set optional attributes
		if (keepAttributes)
			for (AttributeDescriptor attr : schemaOut.getAttributeDescriptors()) {
				if (attr.getLocalName().equals(geomName))
					continue;
				sfTypeBuilder.add(attr);
			}
		if (attrsToStat != null && !attrsToStat.isEmpty())
			for (String attrToStat : attrsToStat)
				for (StatisticOperation statToDo : statsToDo)
					sfTypeBuilder.add(attrToStat + "-" + statToDo, Double.class);
		SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
		DefaultFeatureCollection result = new DefaultFeatureCollection();
		try (SimpleFeatureIterator polyIt = polygonsCollec.features()) {
			while (polyIt.hasNext()) {
				SimpleFeature poly = polyIt.next();
				// set already existing polygon attributes
				sfb.set(geomName, poly.getDefaultGeometry());
				if (keepAttributes)
					for (AttributeDescriptor attr : schemaOut.getAttributeDescriptors()) {
						if (attr.getLocalName().equals(geomName))
							continue;
						sfb.set(attr.getLocalName(), poly.getAttribute(attr.getLocalName()));
					}
				// select intersecting points
				SimpleFeatureCollection pts = Collec.selectIntersection(pointsCollec, (Geometry) poly.getDefaultGeometry());
				sfb.set("count", pts.size());
				if (attrsToStat != null && !attrsToStat.isEmpty())
					for (String attrToStat : attrsToStat) {
						if (statsToDo.contains(StatisticOperation.MEAN))
							sfb.set(attrToStat + "-" + StatisticOperation.MEAN,
									Collec.getCollectionAttributeDescriptiveStat(pts, attrToStat, StatisticOperation.MEAN));
						if (statsToDo.contains(StatisticOperation.MEDIAN))
							sfb.set(attrToStat + "-" + StatisticOperation.MEDIAN,
									Collec.getCollectionAttributeDescriptiveStat(pts, attrToStat, StatisticOperation.MEDIAN));
						if (statsToDo.contains(StatisticOperation.STANDEV))
							sfb.set(attrToStat + "-" + StatisticOperation.STANDEV,
									Collec.getCollectionAttributeDescriptiveStat(pts, attrToStat, StatisticOperation.STANDEV));
						if (statsToDo.contains(StatisticOperation.SUM))
							sfb.set(attrToStat + "-" + StatisticOperation.SUM,
									Collec.getCollectionAttributeDescriptiveStat(pts, attrToStat, StatisticOperation.SUM));
						if (statsToDo.contains(StatisticOperation.CENSUS))
							sfb.set(attrToStat + "-" + StatisticOperation.CENSUS, Collec.getEachUniqueFieldFromSFC(pts, attrToStat));
					}
				result.add(sfb.buildFeature(Attribute.makeUniqueId()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
}
