package fr.ign.artiscales.tools.io;

import com.fasterxml.jackson.core.*;
import com.opencsv.CSVReader;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.io.csv.Csv;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class Json {
	public static void main(String[] args) throws IOException {
		transformCsvToJSON(new File("/home/mcolomb/inf.csv"),new File("/home/mcolomb/inf.json"));
	}

	public static void transformCsvToJSON(File csvFile, File outJSONFile) throws IOException {
		CSVReader csv = Csv.getCSVReader(csvFile);
		String[] fline = csv.readNext();
		JsonFactory factory = new JsonFactory();
		JsonGenerator generator = factory.createGenerator(outJSONFile, JsonEncoding.UTF8);
        generator.useDefaultPrettyPrinter();
        int iLat = Attribute.getLatIndice(fline);
        int iLon = Attribute.getLongIndice(fline);
        int iDay = Attribute.getIndice(fline, "date");
		generator.writeStartArray();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		Calendar cal = Calendar.getInstance();
		System.out.println("Current Date: "+sdf.format(cal.getTime()));
		for (String[] line : csv.readAll()){
			cal.add(Calendar.DAY_OF_MONTH, Integer.parseInt(line[iDay]));
			String newDate = sdf.format(cal.getTime());
            System.out.println("newDate = " + newDate);
            generator.writeStartObject(); // Start with left brace i.e. {
			generator.writeStringField("date",newDate);
			generator.writeNumberField("lat", Double.parseDouble(line[iLat]));
			generator.writeNumberField("lon", Double.parseDouble(line[iLon]));
			generator.writeEndObject(); // End with right brace i.e }
            cal.add(Calendar.DAY_OF_MONTH, -Integer.parseInt(line[iDay]));
        }
        generator.writeEndArray();
		generator.close();
		csv.close();
	}

	public static HashMap<String, String> getHeaderJson(File f) throws IOException {
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(f);
		JsonToken token = parser.nextToken();
		HashMap<String, String> header = new HashMap<>();
		boolean write = false;
		try {
			while (!parser.isClosed()) {
				token = parser.nextToken();
				if (token == JsonToken.START_OBJECT && parser.getCurrentName().equals("header"))
					write = true;
				if (token == JsonToken.END_OBJECT && parser.getCurrentName().equals("header"))
					break;
				if (token == JsonToken.FIELD_NAME && write) {
					String key = parser.getCurrentName();
					token = parser.nextToken();
					header.put(key, parser.getText());
				}
			}
		} catch (NullPointerException np) {
			np.printStackTrace();
			System.out.println("Invalid header");
		}
		return header;
	}

	public static HashMap<String, Object> getFirstObject(File in) throws IOException {
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(in);
		JsonToken token = parser.nextToken();
		HashMap<String, Object> firstObject = new HashMap<String, Object>();
		boolean write = false;
		String firstObjectName = "";
		try {
			while (!parser.isClosed()) {
				token = parser.nextToken();
				if (token == JsonToken.START_OBJECT && (parser.getCurrentName() == null || !parser.getCurrentName().equals("header"))) {
					firstObjectName = parser.getCurrentName();
					write = true;
				}
				if (token == JsonToken.END_OBJECT && parser.getCurrentName() == firstObjectName)
					// IntelliJ tells == should be replaced by equals() and I tend to agree with that, but then it does't work... todo resolve that mystery someday
					break;
				if (token == JsonToken.FIELD_NAME && write) {
					String key = parser.getCurrentName();
					token = parser.nextToken();
					firstObject.put(key, parser.getText());
				}
			}
		} catch (NullPointerException np) {
			np.printStackTrace();
			System.out.println("Invalid first object");
		}
		return firstObject;
	}
}
