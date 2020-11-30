package fr.ign.artiscales.tools.carto;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import java.util.List;

public class CountPointInPolygon {
	/**
	 * Count points that are included in every polygons of a collection. Doesn't keep the polygon attributes.
	 * @param pointsCollec Point collection
	 * @param polygonsCollec Polygon collection
	 * @return the Polygon collection with a new field <i>count</i>.
	 */
	public static SimpleFeatureCollection countPointInPolygon(SimpleFeatureCollection pointsCollec, SimpleFeatureCollection polygonsCollec) {
		return countPointInPolygon(pointsCollec, polygonsCollec, false, null, null,null);
	}

	/**
	 * Count points that are included in every polygons of a collection
	 * @param pointsCollec Point collection
	 * @param polygonsCollec Polygon collection
	 * @param keepAttributes If true, keep every attributes of the input polygon collection
	 * @param attrsToDescriptiveStat List of attributes for which descriptive statistics will be calculated
	 * @param attrsToCensusStat List of attributes for which every occurrence will be counted
	 * @param statsToDo Which descriptive statistics are calculated
	 * @return the Polygon collection with a new field <i>count</i> and new fields depending on the attributes calculations
	 */
	public static SimpleFeatureCollection countPointInPolygon(SimpleFeatureCollection pointsCollec, SimpleFeatureCollection polygonsCollec,
			boolean keepAttributes, List<String> attrsToDescriptiveStat,List<String> attrsToCensusStat, List<StatisticOperation> statsToDo) {

		// enrich the output wanted schema with wanted stats about attributes
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		SimpleFeatureType schemaOut = polygonsCollec.getSchema();
		sfTypeBuilder.setName(schemaOut.getName()+"-counted-"+pointsCollec.getSchema().getName());
		String geomName = schemaOut.getGeometryDescriptor().getLocalName();
//		sfTypeBuilder.add(geomName, MultiPolygon.class);
//		sfTypeBuilder.setDefaultGeometry(geomName);
		sfTypeBuilder.setCRS(schemaOut.getCoordinateReferenceSystem());
		GeometryDescriptor gd = schemaOut.getGeometryDescriptor();
		AttributeTypeBuilder attributeBuilder = new AttributeTypeBuilder();
		attributeBuilder.init(gd);
		attributeBuilder.setCRS(schemaOut.getCoordinateReferenceSystem());
		GeometryDescriptor att = (GeometryDescriptor) attributeBuilder.buildDescriptor(gd.getLocalName());
		sfTypeBuilder.add(att);
		sfTypeBuilder.setDefaultGeometry(att.getLocalName());

		sfTypeBuilder.add("count", Integer.class);
		// set optional attributes
		if (keepAttributes)
			for (AttributeDescriptor attr : schemaOut.getAttributeDescriptors()) {
				if (attr.getLocalName().equals(geomName))
					continue;
				sfTypeBuilder.add(attr);
			}
		// Count/census stats
		if (attrsToCensusStat != null && !attrsToCensusStat.isEmpty())
			for (String attrToCensusStat : attrsToCensusStat)
				for (String attr : Collec.getEachUniqueFieldFromSFC(pointsCollec, attrToCensusStat))
					sfTypeBuilder.add(attrToCensusStat + "-" + attr, Integer.class);
		// Descriptive stats
		if (attrsToDescriptiveStat != null && !attrsToDescriptiveStat.isEmpty())
			for (String attrToStat : attrsToDescriptiveStat)
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

				//Counts values
				if (attrsToCensusStat != null && !attrsToCensusStat.isEmpty())
					for (String attrToCensusStat : attrsToCensusStat)
						for (String attr : Collec.getEachUniqueFieldFromSFC(pts, attrToCensusStat))
							sfb.set(attrToCensusStat + "-" + attr, Collec.getCollectionAttributeCount(pts, attrToCensusStat, attr));
				//Create statistics
				if (attrsToDescriptiveStat != null && !attrsToDescriptiveStat.isEmpty())
					for (String attrToStat : attrsToDescriptiveStat) {
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