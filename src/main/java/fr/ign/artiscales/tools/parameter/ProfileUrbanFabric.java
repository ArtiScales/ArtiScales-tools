package fr.ign.artiscales.tools.parameter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parameters describing an urban fabric. One different objects for each of the simulated urban scene. Must be set in a .json file and then parse into either constructor or static
 * parser method.
 *
 * @author Maxime Colomb
 */
public class ProfileUrbanFabric {
    static String profileFolder;
    String nameBuildingType;
    double maximalArea, minimalArea, minimalWidthContactRoad, streetWidth, largeStreetWidth, maxDepth, maxDistanceForNearestRoad, maxWidth;
    int largeStreetLevel, decompositionLevelWithoutStreet;
    double lenDriveway, noise, harmonyCoeff = 0.5;
    boolean generatePeripheralRoad;

    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double minimalWidthContactRoad,
                              double smallStreetWidth, double largeStreetWidth, int largeStreetLevel, int decompositionLevelWithoutStreet) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = minimalWidthContactRoad;
        this.streetWidth = smallStreetWidth;
        this.largeStreetWidth = largeStreetWidth;
        this.largeStreetLevel = largeStreetLevel;
        this.decompositionLevelWithoutStreet = decompositionLevelWithoutStreet;
    }

    /**
     * For every parameter use
     */
    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double minimalWidthContactRoad,
                              double smallStreetWidth, double largeStreetWidth, int largeStreetLevel, int decompositionLevelWithoutStreet, double lenDriveway,
                              double maxDepth, double maxDistanceForNearestRoad, double maxWidth, boolean generatePeripheralRoad) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = minimalWidthContactRoad;
        this.streetWidth = smallStreetWidth;
        this.largeStreetWidth = largeStreetWidth;
        this.largeStreetLevel = largeStreetLevel;
        this.decompositionLevelWithoutStreet = decompositionLevelWithoutStreet;
        this.lenDriveway = lenDriveway;
        this.maxDepth = maxDepth;
        this.maxDistanceForNearestRoad = maxDistanceForNearestRoad;
        this.maxWidth = maxWidth;
        this.generatePeripheralRoad = generatePeripheralRoad;
    }

    public void setMaximalArea(double maximalArea) {
        this.maximalArea = maximalArea;
    }

    public void setMinimalArea(double minimalArea) {
        this.minimalArea = minimalArea;
    }

    /**
     * For Straight Skeleton
     */
    public ProfileUrbanFabric(String nameBuildingType, double minimalArea,
                              double maxDepth, double maxDistanceForNearestRoad, double minWidth,
                              double maxWidth, double streetWidth, boolean generatePeripheralRoad) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.minimalArea = minimalArea;
        this.streetWidth = streetWidth;
        this.maxDepth = maxDepth;
        this.maxDistanceForNearestRoad = maxDistanceForNearestRoad;
        this.maxWidth = maxWidth;
        this.minimalWidthContactRoad = minWidth;
        this.generatePeripheralRoad = generatePeripheralRoad;
    }

    /**
     * Builder for Oriented Bounding Box
     */
    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double maximalWidth, double streetWidth,
                              int largeStreetLevel, int decompositionLevelWithoutStreet) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = maximalWidth;
        this.largeStreetWidth = streetWidth;
        this.streetWidth = streetWidth;
        this.largeStreetLevel = largeStreetLevel;
        this.decompositionLevelWithoutStreet = decompositionLevelWithoutStreet;
    }

    /**
     * Builder for flag cut
     */
    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double maximalWidth, double lenDriveway) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = maximalWidth;
        this.lenDriveway = lenDriveway;
    }

    public ProfileUrbanFabric() {
    }

    public ProfileUrbanFabric(String[] firstLine, String[] line) {
        int iMaximalArea = 999, iDecompositionLevelWithoutStreet = 999, iLargeStreetLevel = 999, iStreetWidth = 999,
                iHarmonyCoeff = 999, iLargeStreetWidth = 999, iMinimalWidthContactRoad = 999;
        for (int i = 0; i < firstLine.length; i++) {
            String index = firstLine[i];
            switch (index) {
                case "maximalArea":
                    iMaximalArea = i;
                    break;
                case "decompositionLevelWithoutStreet":
                    iDecompositionLevelWithoutStreet = i;
                    break;
                case "largeStreetLevel":
                    iLargeStreetLevel = i;
                    break;
                case "streetWidth":
                    iStreetWidth = i;
                    break;
                case "harmonyCoeff":
                    iHarmonyCoeff = i;
                    break;
                case "largeStreetWidth":
                    iLargeStreetWidth = i;
                    break;
                case "minimalWidthContactRoad":
                    iMinimalWidthContactRoad = i;
                    break;
            }
        }
        this.maximalArea = Double.parseDouble(line[iMaximalArea]);
        this.minimalWidthContactRoad = Double.parseDouble(line[iMinimalWidthContactRoad]);
        this.streetWidth = Double.parseDouble(line[iStreetWidth]);
        this.largeStreetWidth = Double.parseDouble(line[iLargeStreetWidth]);
        this.largeStreetLevel = Integer.parseInt(line[iLargeStreetLevel]);
        this.decompositionLevelWithoutStreet = Integer.parseInt(line[iDecompositionLevelWithoutStreet]);
        this.harmonyCoeff = Double.parseDouble(line[iHarmonyCoeff]);
    }

    public static String getProfileFolder() {
        return profileFolder;
    }

    public static ProfileUrbanFabric convertJSONtoProfile(File jsonFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        InputStream fileInputStream = new FileInputStream(jsonFile);
        ProfileUrbanFabric profile = mapper.readValue(fileInputStream, ProfileUrbanFabric.class);
        fileInputStream.close();
        return profile;
    }


    public boolean isGeneratePeripheralRoad() {
        return generatePeripheralRoad;
    }

    public void setGeneratePeripheralRoad(boolean generatePeripheralRoad) {
        this.generatePeripheralRoad = generatePeripheralRoad;
    }

    public String getNameBuildingType() {
        return nameBuildingType;
    }

    public double getMaximalArea() {
        return maximalArea;
    }

    public double getMinimalArea() {
        return minimalArea;
    }

    public double getMinimalWidthContactRoad() {
        return minimalWidthContactRoad;
    }

    public int getDecompositionLevelWithoutStreet() {
        return decompositionLevelWithoutStreet;
    }

    public double getLenDriveway() {
        return lenDriveway;
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

    public void setLargeStreetWidth(double newStreetWidth) {
        this.largeStreetWidth = newStreetWidth;
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
        return "ProfileBuilding " + nameBuildingType + " [maximalArea=" + maximalArea + ", minimalArea=" + minimalArea + ", minimalWidthContactRoad="
                + minimalWidthContactRoad + ", smallStreetWidth=" + streetWidth + ", largeStreetWidth=" + largeStreetWidth + ", largeStreetLevel="
                + largeStreetLevel + ", decompositionLevelWithoutStreet=" + decompositionLevelWithoutStreet + ", lenDriveway=" + lenDriveway + "]";
    }

    public File exportToJSON(File f) throws IOException {
        String json = new ObjectMapper().writeValueAsString(this);
        FileWriter w = new FileWriter(f);
        w.append(json);
        w.close();
        return f;
    }

    public double getNoise() {
        return noise;
    }

    public void setNoise(double noise) {
        this.noise = noise;
    }

    public double getHarmonyCoeff() {
        return harmonyCoeff;
    }

    public void setHarmonyCoeff(double harmonyCoeff) {
        this.harmonyCoeff = harmonyCoeff;
    }

    public double getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(double maxDepth) {
        this.maxDepth = maxDepth;
    }

    public double getMaxDistanceForNearestRoad() {
        return maxDistanceForNearestRoad;
    }

    public double getMaxWidth() {
        return maxWidth;
    }
}
