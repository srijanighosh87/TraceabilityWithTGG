package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emoflon.ibex.tgg.run.pluginstoexceltgg.SYNC_App;

import com.kaleidoscope.core.auxiliary.xmi.artefactadapter.XMIArtefactAdapter;

import Simpleexcel.Cell;
import Simpleexcel.ExcelElement;
import Simpleexcel.File;
import Simpleexcel.Row;
import Simpleexcel.Sheet;

public class XmlToExcelConversion {

	public HashMap<Row, List<Row>> pluginBlockMap = new HashMap<Row, List<Row>>();
	public List<Row> pluginHeaderList = new ArrayList<Row>();
	public List<Row> sortedPluginHeaderList = new ArrayList<Row>();
	private String excelPath = "";

	public void convert(String workspacePath, String excelGenerationPath) throws IOException {
		this.excelPath = excelGenerationPath;
		// XmlToExcelConversion xmlToExcelConversionMainClass = new
		// XmlToExcelConversion();

		// convert XML artefact to Simpletreemodel and save it as src.xmi
		this.convertXMLToSimpleTree(workspacePath);

		// call SYNC_APP
		BasicConfigurator.configure();
		SYNC_App sync = new SYNC_App(true);
		sync.executeSync(sync);

		// post processing
		ExcelElement excelElement = this.readXMIModel();
		this.postProcess(excelElement, this);

		// convert simpleExcelmodel to Excel artefact
		this.convertSimpleExcelToExcel(excelElement);
	}

	/**
	 * @param excelElement
	 *            Method for post-processing: connects missing links between rows
	 * @param xmlToExcelConversionMainClass
	 */
	private void postProcess(ExcelElement excelElement, XmlToExcelConversion xmlToExcelConversionMainClass) {
		System.out.println("Starting post-processing ... ");
		// System.out.println(excelElement);

		File file = (File) excelElement;
		Sheet sheet = file.getSheet().size() > 0 ? file.getSheet().get(0) : null;
		if (sheet != null) {
			List<Row> rowList = sheet.getRowobject().size() > 0 ? sheet.getRowobject() : null;

			// generate map for plugin_block_rows
			if (rowList != null) {
				for (Row row : rowList) {
					if (row.getPrevRow() == null) {
						// System.out.println("Header Row: "+ row.getBackgroundColor());
						Row header = row;
						// define extensionRowList
						List<Row> extensionRowList = new ArrayList<Row>();
						while (row.getNextRow() != null) {
							// System.out.println(" Extension Row : "+ row.getBackgroundColor());
							extensionRowList.add(row.getNextRow());
							row = row.getNextRow();
						}

						pluginBlockMap.put(header, extensionRowList);
						pluginHeaderList.add(header);
					}
				}
			}

			// sort the plugin header list alphabetically by the name of the plugin in the
			// first cell of the row
			sortList();

			// connect plugin blocks
			if (!pluginBlockMap.isEmpty() && sortedPluginHeaderList.size() > 0) {
				for (int i = 0; i < sortedPluginHeaderList.size(); i++) {
					Row headerRow = sortedPluginHeaderList.get(i);
					List<Row> extensionList = pluginBlockMap.get(headerRow);
					if (extensionList.size() > 0) {
						Row lastExtenstionRowForThisHeader = extensionList.get(extensionList.size() - 1);
						if (sortedPluginHeaderList.size() > (i + 1)) {
							lastExtenstionRowForThisHeader.setNextRow(sortedPluginHeaderList.get(i + 1));
						}
					}

				}
			}
		}

		xmlToExcelConversionMainClass.storeSimpleTreeModelToXMI(excelElement, xmlToExcelConversionMainClass);

		System.out.println("Post-processing End ... ");
	}

	/**
	 * Sort the plugins alphabetically
	 */
	private void sortList() {

		// create map<nameFromCell, RowObject> from the list
		TreeMap<String, Row> dataMap = new TreeMap<String, Row>();
		for (Row row : pluginHeaderList) {
			Cell cellWithPluginName = row.getCell().size() > 0 ? row.getCell().get(0) : null;
			if (cellWithPluginName != null) {
				dataMap.put(cellWithPluginName.getText(), row);
			}
		}

		// add excel header to the sorted list
		sortedPluginHeaderList.add(0, pluginHeaderList.get(0));

		for (Map.Entry<String, Row> entry : dataMap.entrySet()) {
			Row pluginHeaderRow = entry.getValue();
			if (pluginBlockMap.containsKey(pluginHeaderRow) && (pluginBlockMap.get(pluginHeaderRow).size() > 0)) {
				sortedPluginHeaderList.add(pluginHeaderRow);
			} else {
				List<Cell> headerCells = pluginHeaderRow.getCell();
				checkIfPluginWithNoExtenstion(headerCells, pluginHeaderRow);
			}
		}

		// add connection: header to first plugin
		if (sortedPluginHeaderList.size() > 1) {
			sortedPluginHeaderList.get(0).setNextRow(sortedPluginHeaderList.get(1));
		}

		System.out.println();
	}

	/**
	 * this condition removes plug-in header from the model which doesn't have any
	 * extensions
	 * 
	 * @param headerCells
	 * @param pluginHeaderRow
	 */
	private void checkIfPluginWithNoExtenstion(List<Cell> headerCells, Row pluginHeaderRow) {
		if (headerCells.get(0) != null && headerCells.get(0).getText() != null) {
			if (!headerCells.get(0).getText().equalsIgnoreCase("")
					&& (headerCells.get(1) == null || headerCells.get(1).getText() == null
							|| headerCells.get(1).getText().equalsIgnoreCase(""))
					&& (headerCells.get(2) == null || headerCells.get(2).getText() == null
							|| headerCells.get(2).getText().equalsIgnoreCase(""))
					&& (headerCells.get(3) == null || headerCells.get(3).getText() == null
							|| headerCells.get(3).getText().equalsIgnoreCase(""))) {

				Sheet sheet = pluginHeaderRow.getSheet();
				sheet.getRowobject().remove(pluginHeaderRow);
			}
		}

	}

	/**
	 * Method for saving the model as src.xmi
	 * 
	 * @param xmlToExcelConversionMainClass
	 * 
	 * @param xmlAdapter
	 * @param generatedSimpleTreeModelPath
	 */
	private void storeSimpleTreeModelToXMI(ExcelElement excelElement,
			XmlToExcelConversion xmlToExcelConversionMainClass) {

		XMIArtefactAdapter<ExcelElement> xmiArtefactAdapter = new XMIArtefactAdapter<ExcelElement>(
				Paths.get("./Resources/postProcessedExcel/trg_processed.xmi"));
		xmiArtefactAdapter.setModel(excelElement);
		xmiArtefactAdapter.unparse();

		System.out.println("Parsing completed ...");
	}

	/**
	 * reads xmi file and stores in model
	 * 
	 * @return
	 */
	private ExcelElement readXMIModel() {
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource resource = rs.getResource(URI.createURI(CONSTANTS.SIMPLE_EXCEL_XMI_PATH), true);
		try {
			ExcelElement obj = (ExcelElement) resource.getContents().get(0);
			return obj;
		} catch (Exception e) {
			System.out.println("ERROR :: "+ e);
		}
		return null;
	}

	/**
	 * 
	 */
	private void convertXMLToSimpleTree(String workspacePath) {
		XmlToOrFromSimpleTree xmLtoSimpleTree = new XmlToOrFromSimpleTree();
		xmLtoSimpleTree.convertXMLToSimpleTree(workspacePath);
	}

	/**
	 * @param excelElement
	 */
	private void convertSimpleExcelToExcel(ExcelElement excelElement) {
		ExcelToOrFromSimpleExcel simpleExcelToExcel = new ExcelToOrFromSimpleExcel();
		simpleExcelToExcel.convertSimpleExcelToExcel(excelElement, excelPath);
	}

}
