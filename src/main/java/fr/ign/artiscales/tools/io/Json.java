package fr.ign.artiscales.tools.io;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class Json {
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
		HashMap<String, Object> firstObject = new HashMap<>();
		boolean write = false;
		String firstObjectName = "";
		try {
			while (!parser.isClosed()) {
				token = parser.nextToken();
				if (token == JsonToken.START_OBJECT && (parser.getCurrentName() == null || !parser.getCurrentName().equals("header"))) {
					firstObjectName = parser.getCurrentName();
					write = true;
				}
				if (token == JsonToken.END_OBJECT && parser.getCurrentName().equals(firstObjectName))
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
