package fr.ign.artiscales.tools.io;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class XmlGen {

	File fileName;

	public XmlGen(File filename, String scenarName) throws IOException {
		fileName = filename;
		FileWriter writer = new FileWriter(fileName, false);
		writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
		writer.append("\n");
		beginBalise(scenarName);
		writer.close();
	}

	public void addLine(String balise, String argument) throws IOException {
		FileWriter writer = new FileWriter(fileName, true);
		String line = "<" + balise + ">" + argument + "</" + balise + ">";
		writer.append(line + "\n");
		writer.close();
	}

	public void beginBalise(String balise) throws IOException {
		FileWriter writer = new FileWriter(fileName, true);
		String line = "<" + balise + ">";
		writer.append(line + "\n");
		writer.close();
	}

	public void endBalise(String balise) throws IOException {
		FileWriter writer = new FileWriter(fileName, true);
		String line = "</" + balise + ">";
		writer.append(line + "\n");
		writer.close();
	}

}
