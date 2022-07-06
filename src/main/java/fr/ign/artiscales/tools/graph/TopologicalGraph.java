package fr.ign.artiscales.tools.graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;

public class TopologicalGraph {
  private static final boolean DEBUG = false;
  List<Node> nodes = new ArrayList<>();
  List<Edge> edges = new ArrayList<>();
  List<Face> faces = new ArrayList<>();
  static int SRIDNumber = 2154;
  public List<Node> getNodes() {
    return nodes;
  }

  public void setNodes(List<Node> nodes) {
    this.nodes = nodes;
  }

  public List<Edge> getEdges() {
    return edges;
  }

  public void setEdges(List<Edge> edges) {
    this.edges = edges;
  }

  public List<Face> getFaces() {
    return faces;
  }

  public void setFaces(List<Face> faces) {
    this.faces = faces;
  }
  public static void setSRID(int srid) {
    SRIDNumber= srid;
  }
  
  public static int getSRID() {
    return SRIDNumber;
  }

  public List<Edge> edgesOf(Node node) {
    return edgesOf(node, this.edges);
  }

  public List<Edge> edgesOf(Node node, List<Edge> edges) {
    return edges.stream().filter(e -> (e.getOrigin() == node || e.getTarget() == node)).collect(Collectors.toList());
  }

  public Edge next(Node node, Edge edge, List<Edge> edges) {
    return edgesOf(node, edges).stream().filter(e -> e != edge).findAny().orElse(null);
  }

  public Node getCommonNode(Edge e1, Edge e2) {
    if (e1.getOrigin() == e2.getOrigin() || e1.getOrigin() == e2.getTarget())
      return e1.getOrigin();
    return e1.getTarget();
  }
  
  public static <G extends Geometry, E extends GraphElement<G>> void export(List<E> feats, File fileOut,	Class<? extends Geometry> geomType) {
	    if (DEBUG)
            System.out.println("save " + feats.size() + " to " + fileOut);
		if (feats.isEmpty())
			return;
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode("EPSG:" + SRIDNumber));
//			sfTypeBuilder.setCRS(CRS.decode("EPSG:2154" ));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName(fileOut.getName());
		sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), geomType);
		sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		List<String> attributes = feats.get(0).getAttributes();
		for (String attribute : attributes)
			sfTypeBuilder.add(attribute, String.class);
		SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
		DefaultFeatureCollection dfc = new DefaultFeatureCollection();
		for (E element : feats) {
			builder.set(CollecMgmt.getDefaultGeomName(), element.getGeometry());
//			for (int i = 0; i < attributes.size(); i++) {
//			System.out.println("elttt : "+element.getAttribute(attributes.get(i)));
//				builder.set(i, element.getAttribute(attributes.get(i)));
//			}
			dfc.add(builder.buildFeature(Attribute.makeUniqueId()));
		}
		try {
            CollecMgmt.exportSFC(dfc, fileOut);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
