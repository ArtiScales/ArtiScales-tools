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
    double maximalArea, minimalArea, minimalWidthContactRoad, laneWidth, streetWidth, maxDepth, maxDistanceForNearestRoad, maxWidth;
    int streetLane, blockShape;
    double lenDriveway, noise, harmonyCoeff = 0.5;

    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double minimalWidthContactRoad,
                              double smallStreetWidth, double largeStreetWidth, int largeStreetLevel, int blockShape) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = minimalWidthContactRoad;
        this.laneWidth = smallStreetWidth;
        this.streetWidth = largeStreetWidth;
        this.streetLane = largeStreetLevel;
        this.blockShape = blockShape;
    }

    /**
     * For every parameter use
     */
    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double minimalWidthContactRoad,
                              double laneWidth, double streetWidth, int streetLane, int blockShape, double lenDriveway,
                              double maxDepth, double maxDistanceForNearestRoad, double maxWidth) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = minimalWidthContactRoad;
        this.laneWidth = laneWidth;
        this.streetWidth = streetWidth;
        this.streetLane = streetLane;
        this.blockShape = blockShape;
        this.lenDriveway = lenDriveway;
        this.maxDepth = maxDepth;
        this.maxDistanceForNearestRoad = maxDistanceForNearestRoad;
        this.maxWidth = maxWidth;
    }

    /**
     * For Straight Skeleton
     */
    public ProfileUrbanFabric(String nameBuildingType, double minimalArea, double maxDepth, double maxDistanceForNearestRoad, double minWidth, double maxWidth, double streetWidth) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.minimalArea = minimalArea;
        this.laneWidth = streetWidth;
        this.maxDepth = maxDepth;
        this.maxDistanceForNearestRoad = maxDistanceForNearestRoad;
        this.maxWidth = maxWidth;
        this.minimalWidthContactRoad = minWidth;
    }

    /**
     * Builder for Oriented Bounding Box
     */
    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double maximalWidth, double streetWidth, int largeStreetLevel, int blockShape) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = maximalWidth;
        this.streetWidth = streetWidth;
        this.laneWidth = streetWidth;
        this.streetLane = largeStreetLevel;
        this.blockShape = blockShape;
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
        int iMaximalArea = 999, iBlockShape = 999, iStreetLane = 999, iLaneWidth = 999,
                iHarmonyCoeff = 999, iStreetWidth = 999, iMinimalWidthContactRoad = 999;
        for (int i = 0; i < firstLine.length; i++) {
            String index = firstLine[i];
            switch (index) {
                case "maximalArea":
                    iMaximalArea = i;
                    break;
                case "blockShape":
                    iBlockShape = i;
                    break;
                case "streetLane":
                    iStreetLane = i;
                    break;
                case "laneWidth":
                    iLaneWidth = i;
                    break;
                case "harmonyCoeff":
                    iHarmonyCoeff = i;
                    break;
                case "streetWidth":
                    iLaneWidth = i;
                    break;
                case "minimalWidthContactRoad":
                    iMinimalWidthContactRoad = i;
                    break;
            }
        }
        this.maximalArea = Double.parseDouble(line[iMaximalArea]);
        this.minimalWidthContactRoad = Double.parseDouble(line[iMinimalWidthContactRoad]);
        this.laneWidth = Double.parseDouble(line[iLaneWidth]);
        this.streetWidth = Double.parseDouble(line[iLaneWidth]);
        this.streetLane = Integer.parseInt(line[iStreetLane]);
        this.blockShape = Integer.parseInt(line[iBlockShape]);
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

    public String getNameBuildingType() {
        return nameBuildingType;
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

    public double getMinimalWidthContactRoad() {
        return minimalWidthContactRoad;
    }

    public int getBlockShape() {
        return blockShape;
    }

    public void setBlockShape(int blockShape) {
        this.blockShape = blockShape;
    }

    public double getLenDriveway() {
        return lenDriveway;
    }

    public double getLaneWidth() {
        return laneWidth;
    }

    public double getStreetWidth() {
        if (streetWidth != 0.0) {
            return streetWidth;
        } else {
            return laneWidth;
        }
    }

    public void setStreetWidth(double newStreetWidth) {
        this.streetWidth = newStreetWidth;
    }

    public int getStreetLane() {
        if (streetLane != 0) {
            return streetLane;
        } else {
            return 999;
        }
    }

    @Override
    public String toString() {
        return "ProfileBuilding " + nameBuildingType + " [maximalArea=" + maximalArea + ", minimalArea=" + minimalArea + ", minimalWidthContactRoad="
                + minimalWidthContactRoad + ", smallStreetWidth=" + laneWidth + ", largeStreetWidth=" + streetWidth + ", largeStreetLevel="
                + streetLane + ", decompositionLevelWithoutStreet=" + blockShape + ", lenDriveway=" + lenDriveway + "]";
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
