package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class MainClass {
	public static boolean CONVERSION_DIRECTION;

	public static void main(String[] args) {
		Logger.getRootLogger().setLevel(Level.DEBUG);

		// true: XML_TO_EXCEL; false:EXCEL_TO_XML
		CONVERSION_DIRECTION = true;

		if (CONVERSION_DIRECTION) {
			try {
				System.out.println("Transforming Plugin Workspace to EXCEL");
				new XmlToExcelConversion().convert("C:\\Users\\Srijani\\Desktop\\Workspace",
						"C:\\Users\\Srijani\\Desktop\\Master_thesis_code_test\\Relationship.xlsx");
			} catch (IOException e) {
				System.out.println("Some error occurred ..." + e);
			}
		} else {
			try {
				System.out.println("Transforming EXCEL to Plugin Workspace");
				new ExcelToXmlConversion().convert(CONSTANTS.EXCEL_PATH);
			} catch (IOException e) {
				System.out.println("Some error occurred ...");
			}
		}
	}

}
