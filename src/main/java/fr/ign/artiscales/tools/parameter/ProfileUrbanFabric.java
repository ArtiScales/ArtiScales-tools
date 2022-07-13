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
    int streetLane, blockShape, approxNumberParcelPerBlock;
    double drivewayWidth, irregularityCoeff, harmonyCoeff = 0.5;

    /**
     * For every parameter use (or OBBThenSS)
     */
    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double minimalWidthContactRoad,
                              double laneWidth, double streetWidth, int streetLane, int blockShape, double drivewayWidth, double maxDepth,
                              double maxDistanceForNearestRoad, double maxWidth, int approxNumberParcelPerBlock, double harmonyCoeff, double irregularityCoeff) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = minimalWidthContactRoad;
        this.laneWidth = laneWidth;
        this.streetWidth = streetWidth;
        this.streetLane = streetLane;
        this.blockShape = blockShape;
        this.drivewayWidth = drivewayWidth;
        this.maxDepth = maxDepth;
        this.maxDistanceForNearestRoad = maxDistanceForNearestRoad;
        this.maxWidth = maxWidth;
        this.approxNumberParcelPerBlock = approxNumberParcelPerBlock;
        this.harmonyCoeff = harmonyCoeff;
        this.irregularityCoeff = irregularityCoeff;
    }

    /**
     * For Straight Skeleton
     */
    public ProfileUrbanFabric(String nameBuildingType, double minimalArea, double maxDepth, double maxDistanceForNearestRoad, double minWidth, double maxWidth, double streetWidth, double irregularityCoeff) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.minimalArea = minimalArea;
        this.laneWidth = streetWidth;
        this.maxDepth = maxDepth;
        this.maxDistanceForNearestRoad = maxDistanceForNearestRoad;
        this.maxWidth = maxWidth;
        this.minimalWidthContactRoad = minWidth;
        this.irregularityCoeff = irregularityCoeff;
    }

    /**
     * Builder for Oriented Bounding Box
     */
    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double minimalWidthContactRoad, double streetWidth, int streetLane, double laneWidth, int blockShape, double harmonyCoeff, double irregularityCoeff) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = minimalWidthContactRoad;
        this.streetWidth = streetWidth;
        this.laneWidth = laneWidth;
        this.streetLane = streetLane;
        this.blockShape = blockShape;
        this.harmonyCoeff = harmonyCoeff;
        this.irregularityCoeff = irregularityCoeff;
    }

    /**
     * Builder for flag division
     */
    public ProfileUrbanFabric(String nameBuildingType, double maximalArea, double minimalArea, double maximalWidth, double drivewayWidth, double harmonyCoeff, double irregularityCoeff) {
        super();
        this.nameBuildingType = nameBuildingType;
        this.maximalArea = maximalArea;
        this.minimalArea = minimalArea;
        this.minimalWidthContactRoad = maximalWidth;
        this.drivewayWidth = drivewayWidth;
        this.harmonyCoeff = harmonyCoeff;
        this.irregularityCoeff = irregularityCoeff;
    }

    public ProfileUrbanFabric() {
    }

    public ProfileUrbanFabric(String[] firstLine, String[] line) {
        for (int i = 0; i < firstLine.length; i++) {
            String index = firstLine[i];
            switch (index) {
                case "maximalArea":
                    this.maximalArea = Double.parseDouble(line[i]);
                    break;
                case "blockShape":
                    this.blockShape = Integer.parseInt(line[i]);
                    break;
                case "streetLane":
                    this.streetLane = Integer.parseInt(line[i]);
                    break;
                case "laneWidth":
                    this.laneWidth = Double.parseDouble(line[i]);
                    break;
                case "streetWidth":
                    this.streetWidth = Double.parseDouble(line[i]);
                    break;
                case "minimalWidthContactRoad":
                    this.minimalWidthContactRoad = Double.parseDouble(line[i]);
                    break;
                case "harmonyCoeff":
                    this.harmonyCoeff = Double.parseDouble(line[i]);
                    break;
                case "noise":
                    this.irregularityCoeff = Double.parseDouble(line[i]);
                    break;
                case "maxDepth":
                    this.maxDepth = Double.parseDouble(line[i]);
                    break;
                case "maxWidth":
                    this.maxWidth = Double.parseDouble(line[i]);
                    break;
                case "maxDistanceForNearestRoad":
                    this.maxDistanceForNearestRoad = Double.parseDouble(line[i]);
                    break;
                case "approxNumberParcelPerBlock":
                    this.approxNumberParcelPerBlock = Integer.parseInt(line[i]);
                    break;
            }
        }
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

    public void setMinimalWidthContactRoad(double minimalWidthContactRoad) {
        this.minimalWidthContactRoad = minimalWidthContactRoad;
    }

    public void setMaxDistanceForNearestRoad(double maxDistanceForNearestRoad) {
        this.maxDistanceForNearestRoad = maxDistanceForNearestRoad;
    }

    public void setMaxWidth(double maxWidth) {
        this.maxWidth = maxWidth;
    }

    public void setStreetLane(int streetLane) {
        this.streetLane = streetLane;
    }

    public void setApproxNumberParcelPerBlock(int approxNumberParcelPerBlock) {
        this.approxNumberParcelPerBlock = approxNumberParcelPerBlock;
    }

    public void setDrivewayWidth(double drivewayWidth) {
        this.drivewayWidth = drivewayWidth;
    }

    public void setLaneWidth(double laneWidth) {
        this.laneWidth = laneWidth;
    }

    public int getApproxNumberParcelPerBlock() {
        return approxNumberParcelPerBlock;
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

    public double getDrivewayWidth() {
        return drivewayWidth;
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
        return "ProfileUrbanFabric{" +
                "nameBuildingType='" + nameBuildingType + '\'' +
                ", maximalArea=" + maximalArea +
                ", minimalArea=" + minimalArea +
                ", minimalWidthContactRoad=" + minimalWidthContactRoad +
                ", laneWidth=" + laneWidth +
                ", streetWidth=" + streetWidth +
                ", maxDepth=" + maxDepth +
                ", maxDistanceForNearestRoad=" + maxDistanceForNearestRoad +
                ", maxWidth=" + maxWidth +
                ", streetLane=" + streetLane +
                ", blockShape=" + blockShape +
                ", approxNumberParcelPerBlock=" + approxNumberParcelPerBlock +
                ", lenDriveway=" + drivewayWidth +
                ", irregularityCoeff=" + irregularityCoeff +
                ", harmonyCoeff=" + harmonyCoeff +
                '}';
    }

    public File exportToJSON(File f) throws IOException {
        String json = new ObjectMapper().writeValueAsString(this);
        FileWriter w = new FileWriter(f);
        w.append(json);
        w.close();
        return f;
    }

    public double getIrregularityCoeff() {
        return irregularityCoeff;
    }

    public void setIrregularityCoeff(double irregularityCoeff) {
        this.irregularityCoeff = irregularityCoeff;
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
