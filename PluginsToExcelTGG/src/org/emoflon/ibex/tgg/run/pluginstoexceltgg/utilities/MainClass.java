package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.IOException;

public class MainClass {
	public static boolean CONVERSION_DIRECTION;

	public static void main(String[] args) {
		CONVERSION_DIRECTION = false;
		if(CONVERSION_DIRECTION) {
			try {
				new XmlToExcelConversion().convert();
			} catch (IOException e) {
				System.out.println("Some error occurred ...");
			}
		} else {
			try {
				new ExcelToXmlConversion().convert();
			} catch (IOException e) {
				System.out.println("Some error occurred ...");
			}
		}
	}

}
