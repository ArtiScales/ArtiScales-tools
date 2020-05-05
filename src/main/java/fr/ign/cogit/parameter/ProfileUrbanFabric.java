package fr.ign.cogit.parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Parameters describing an urban fabric. One different objects for each of the simulated urban scene. 
 * Must be set in a .json file and then parse into either constructor or static parser method.
 * 
 * @author Maxime Colomb
 *
 */
public class ProfileUrbanFabric {
	double maximalArea;
	double minimalArea;
	double maximalWidth;
	double streetWidth;
	double largeStreetWidth;
	int largeStreetLevel;
	int decompositionLevelWithoutStreet;
	double lenDriveway;
	double noise = 0;
	double roadEpsilon = 0.55; 
	
	static String profileFolder;

	public ProfileUrbanFabric(double maximalArea, double minimalArea, double maximalWidth, double smallStreetWidth,
			double largeStreetWidth, int largeStreetLevel, int decompositionLevelWithoutStreet, double lenDriveway) {
		super();
		this.maximalArea = maximalArea;
		this.minimalArea = minimalArea;
		this.maximalWidth = maximalWidth;
		this.streetWidth = smallStreetWidth;
		this.largeStreetWidth = largeStreetWidth;
		this.largeStreetLevel = largeStreetLevel;
		this.decompositionLevelWithoutStreet = decompositionLevelWithoutStreet;
		this.lenDriveway = lenDriveway;
	}

	public ProfileUrbanFabric(int maximalArea, int minimalArea, int maximalWidth, int streetWidth,
			int decompositionLevelWithoutStreet, int lenDriveway) {
		super();
		this.maximalArea = maximalArea;
		this.minimalArea = minimalArea;
		this.maximalWidth = maximalWidth;
		this.largeStreetWidth = streetWidth;
		this.streetWidth = streetWidth;
		this.largeStreetLevel = 999;
		this.decompositionLevelWithoutStreet = decompositionLevelWithoutStreet;
		this.lenDriveway = lenDriveway;
	}

	public ProfileUrbanFabric() {
	}

	public double getMaximalArea() {
		return maximalArea;
	}

	public double getMinimalArea() {
		return minimalArea;
	}

	public double getMaximalWidth() {
		return maximalWidth;
	}

	public int getDecompositionLevelWithoutStreet() {
		return decompositionLevelWithoutStreet;
	}

	public double getLenDriveway() {
		return lenDriveway;
	}

	public static String getProfileFolder() {
		return profileFolder;
	}

	public double getStreetWidth() {
		return streetWidth;
	}

	public double getLargeStreetWidth() {
		if (largeStreetWidth != 0.0) {
			return largeStreetWidth;
		} else {
			return streetWidth;
		}
	}

	public int getLargeStreetLevel() {
		if (largeStreetLevel != 0) {
			return largeStreetLevel;
		} else {
			return 999;
		}
	}

	@Override
	public String toString() {
		return "ProfileBuilding [maximalArea=" + maximalArea + ", minimalArea=" + minimalArea + ", maximalWidth="
				+ maximalWidth + ", smallStreetWidth=" + streetWidth + ", largeStreetWidth=" + largeStreetWidth
				+ ", largeStreetLevel=" + largeStreetLevel + ", decompositionLevelWithoutStreet="
				+ decompositionLevelWithoutStreet + ", lenDriveway=" + lenDriveway + "]";
	}
	
	public static ProfileUrbanFabric convertJSONtoProfile(File jsonFile) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		InputStream fileInputStream = new FileInputStream(jsonFile);
		ProfileUrbanFabric profile = mapper.readValue(fileInputStream, ProfileUrbanFabric.class);
		fileInputStream.close();
		return profile;
	}

	public double getNoise() {
		return noise;
	}

	public void setNoise(double noise) {
		this.noise = noise;
	}

	public double getRoadEpsilon() {
		return roadEpsilon;
	}

	public void setRoadEpsilon(double roadEpsilon) {
		this.roadEpsilon = roadEpsilon;
	}
}
