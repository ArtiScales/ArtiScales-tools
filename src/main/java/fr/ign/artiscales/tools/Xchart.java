package fr.ign.artiscales.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.Histogram;

public class Xchart {
	public static void makeGraphHisto(DescriptiveStatistics graph, File graphDepotFolder, String title, String xTitle, String yTitle, int range)
			throws IOException {
		makeGraphHisto(graph.getSortedValues(), graphDepotFolder, title, xTitle, yTitle, range);
	}

	public static void makeGraphHisto(double[] sortedValues, File graphDepotFolder, String title, String xTitle, String yTitle, int range)
			throws IOException {

		// general settings
		CategoryChart chart = new CategoryChartBuilder().width(450).height(400).title(title).xAxisTitle(xTitle).yAxisTitle(yTitle).build();
		// TODO FIXME l'échelle en x n'est pas respécté pour le second graph..
		ArrayList<Double> arL = new ArrayList<Double>();
		for (double sv : sortedValues)
			arL.add(sv);
		Histogram histo = new Histogram(arL, range, sortedValues[0], sortedValues[sortedValues.length - 1]);
		chart.addSeries(title, histo.getxAxisData(), histo.getyAxisData());

		// Customize Chart
		// chart.getStyler().setLegendPosition(LegendPosition.InsideNW);
		chart.getStyler().setLegendVisible(false);
		chart.getStyler().setHasAnnotations(false);
		chart.getStyler().setXAxisLabelRotation(45);
		chart.getStyler().setXAxisDecimalPattern("####");
		chart.getStyler().setXAxisLogarithmicDecadeOnly(true);
		chart.getStyler().setYAxisLogarithmicDecadeOnly(true);
		BitmapEncoder.saveBitmap(chart, graphDepotFolder + "/" + title, BitmapFormat.PNG);
	}
}
