package fr.ign.cogit.parameter;

public class ProfileBuilding {

	double maximalArea;
	double minimalArea;
	double maximalWidth;
	double streetWidth;
	double largeStreetWidth;
	int largeStreetLevel;
	int decompositionLevelWithoutStreet;
	double lenDriveway;

	static String profileFolder;

	public ProfileBuilding(double maximalArea, double minimalArea, double maximalWidth, double smallStreetWidth,
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

	public ProfileBuilding(int maximalArea, int minimalArea, int maximalWidth, int streetWidth,
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

	public ProfileBuilding() {
	}

	public double getMaximalArea() {
		return maximalArea;
	}

	public void setMaximalArea(double maximalArea) {
		this.maximalArea = maximalArea;
	}

	public double getMinimalArea() {
		return minimalArea;
	}

	public void setMinimalArea(double minimalArea) {
		this.minimalArea = minimalArea;
	}

	public double getMaximalWidth() {
		return maximalWidth;
	}

	public void setMaximalWidth(double maximalWidth) {
		this.maximalWidth = maximalWidth;
	}

	public int getDecompositionLevelWithoutStreet() {
		return decompositionLevelWithoutStreet;
	}

	public void setDecompositionLevelWithoutStreet(int decompositionLevelWithoutStreet) {
		this.decompositionLevelWithoutStreet = decompositionLevelWithoutStreet;
	}

	public double getLenDriveway() {
		return lenDriveway;
	}

	public void setLenDriveway(double lenDriveway) {
		this.lenDriveway = lenDriveway;
	}

	public static String getProfileFolder() {
		return profileFolder;
	}

	public static void setProfileFolder(String profileFolder) {
		ProfileBuilding.profileFolder = profileFolder;
	}

	public double getStreetWidth() {
		return streetWidth;
	}

	public void setStreetWidth(double smallStreetWidth) {
		this.streetWidth = smallStreetWidth;
	}

	public double getLargeStreetWidth() {
		if (largeStreetWidth != 0.0) {
			return largeStreetWidth;
		} else {
			return streetWidth;
		}
	}

	public void setLargeStreetWidth(double largeStreetWidth) {
		this.largeStreetWidth = largeStreetWidth;
	}

	public int getLargeStreetLevel() {
		if (largeStreetLevel != 0) {
			return largeStreetLevel;
		} else {
			return 999;
		}
	}

	public void setLargeStreetLevel(int largeStreetLevel) {
		this.largeStreetLevel = largeStreetLevel;
	}

	@Override
	public String toString() {
		return "ProfileBuilding [maximalArea=" + maximalArea + ", minimalArea=" + minimalArea + ", maximalWidth="
				+ maximalWidth + ", smallStreetWidth=" + streetWidth + ", largeStreetWidth=" + largeStreetWidth
				+ ", largeStreetLevel=" + largeStreetLevel + ", decompositionLevelWithoutStreet="
				+ decompositionLevelWithoutStreet + ", lenDriveway=" + lenDriveway + "]";
	}

}
