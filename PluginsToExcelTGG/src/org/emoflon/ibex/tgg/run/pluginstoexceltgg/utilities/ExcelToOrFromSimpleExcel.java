package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.kaleidoscope.core.auxiliary.simpleexcel.artefactadapter.ExcelArtefactAdapter;
import com.kaleidoscope.core.auxiliary.xmi.artefactadapter.XMIArtefactAdapter;

import Simpleexcel.ExcelElement;
import Simpleexcel.File;

public class ExcelToOrFromSimpleExcel {

	/**
	 * Method for connecting to ExcelAdapter to start SimpleExcel->Excel conversion
	 * 
	 * @param excelElement
	 */
	public void convertSimpleExcelToExcel(ExcelElement excelElement) {
		// calling EXCELAdapter
		Path path = Paths.get(CONSTANTS.EXCEL_PATH);
		ExcelArtefactAdapter excelArtefactAdapter = new ExcelArtefactAdapter(path);
		excelArtefactAdapter.setModel((File) excelElement);
		excelArtefactAdapter.unparse();
		System.out.println("EXCEL file regenrated ...");
	}

	/**
	 * Method for connecting to ExcelAdapter to start Excel->SimpleExcel conversion
	 * 
	 * @return
	 */
	public Optional<File> convertExcelToSimpleExcel() {
		// calling EXCELAdapter
		Path path = Paths.get(CONSTANTS.EXCEL_PATH);
		ExcelArtefactAdapter excelArtefactAdapter = new ExcelArtefactAdapter(path);
		excelArtefactAdapter.parse();
		storeSimpleExcelModelToXMI(excelArtefactAdapter);

		return excelArtefactAdapter.getModel();
	}

	/**
	 * This method stores SimpleExcel Model to .xmi in an intermediate path before
	 * post processing
	 * 
	 * @param excelArtefactAdapter
	 * @param path 
	 */
	public void storeSimpleExcelModelToXMI(ExcelArtefactAdapter excelArtefactAdapter) {
		// store the parsed tree in .xmi file
		Optional<File> model = excelArtefactAdapter.getModel();
		model.ifPresent(m -> {
			XMIArtefactAdapter<ExcelElement> xmiArtefactAdapter = new XMIArtefactAdapter<ExcelElement>(
					Paths.get(CONSTANTS.EXCEL_INTERMEDIATE_MODEL_PATH));
			xmiArtefactAdapter.setModel(m);
			xmiArtefactAdapter.unparse();
		});
		System.out.println("Parsing completed ...");
	}

}
