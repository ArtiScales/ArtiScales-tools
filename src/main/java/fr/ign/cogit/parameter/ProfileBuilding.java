package fr.ign.cogit.parameter;

public class ProfileBuilding {

	int maximalArea;
	int minimalArea;
	int maximalWidth;
	int streetWidth;
	int decompositionLevelWithoutStreet;
	int lenDriveway;

	static String profileFolder;

	
	public ProfileBuilding(int maximalArea, int minimalArea, int maximalWidth, int streetWidth,
			int decompositionLevelWithoutStreet, int lenDriveway) {
		super();
		this.maximalArea = maximalArea;
		this.minimalArea = minimalArea;
		this.maximalWidth = maximalWidth;
		this.streetWidth = streetWidth;
		this.decompositionLevelWithoutStreet = decompositionLevelWithoutStreet;
		this.lenDriveway = lenDriveway;
	}

	public ProfileBuilding() {
	}

	public int getMaximalArea() {
		return maximalArea;
	}

	public void setMaximalArea(int maximalArea) {
		this.maximalArea = maximalArea;
	}

	public int getMinimalArea() {
		return minimalArea;
	}

	public void setMinimalArea(int minimalArea) {
		this.minimalArea = minimalArea;
	}

	public int getMaximalWidth() {
		return maximalWidth;
	}

	public void setMaximalWidth(int maximalWidth) {
		this.maximalWidth = maximalWidth;
	}

	public int getStreetWidth() {
		return streetWidth;
	}

	public void setStreetWidth(int streetWidth) {
		this.streetWidth = streetWidth;
	}

	public int getDecompositionLevelWithoutStreet() {
		return decompositionLevelWithoutStreet;
	}

	public void setDecompositionLevelWithoutStreet(int decompositionLevelWithoutStreet) {
		this.decompositionLevelWithoutStreet = decompositionLevelWithoutStreet;
	}

	public int getLenDriveway() {
		return lenDriveway;
	}

	public void setLenDriveway(int lenDriveway) {
		this.lenDriveway = lenDriveway;
	}

	public static String getProfileFolder() {
		return profileFolder;
	}

	public static void setProfileFolder(String profileFolder) {
		ProfileBuilding.profileFolder = profileFolder;
	}

	@Override
	public String toString() {
		return "ProfileBuilding [maximalArea=" + maximalArea + ", minimalArea=" + minimalArea + ", maximalWidth="
				+ maximalWidth + ", streetWidth=" + streetWidth + ", decompositionLevelWithoutStreet="
				+ decompositionLevelWithoutStreet + ", lenDriveway=" + lenDriveway + "]";
	}

}
