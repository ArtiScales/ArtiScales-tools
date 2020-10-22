package fr.ign.artiscales.tools.io;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class Json {
	public static HashMap<String, String> getHeaderJson(File f) throws JsonParseException, IOException {
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(f);
		JsonToken token = parser.nextToken();
		HashMap<String, String> header = new HashMap<String, String>();
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
}
