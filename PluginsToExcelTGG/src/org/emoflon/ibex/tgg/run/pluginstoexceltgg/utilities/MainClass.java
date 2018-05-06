package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.IOException;

public class MainClass {
	public static boolean CONVERSION_DIRECTION;

	public static void main(String[] args) {
		CONVERSION_DIRECTION = false;
		if(CONVERSION_DIRECTION) {
			try {
				System.out.println("Transforming Plugin Workspace to EXCEL");
				new XmlToExcelConversion().convert();
			} catch (IOException e) {
				System.out.println("Some error occurred ...");
			}
		} else {
			try {
				System.out.println("Transforming EXCEL to Plugin Workspace");
				new ExcelToXmlConversion().convert();
			} catch (IOException e) {
				System.out.println("Some error occurred ...");
			}
		}
	}

}
