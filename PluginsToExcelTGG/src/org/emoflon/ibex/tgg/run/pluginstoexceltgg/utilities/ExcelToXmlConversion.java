package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kaleidoscope.core.auxiliary.xmi.artefactadapter.XMIArtefactAdapter;

import Simpleexcel.Cell;
import Simpleexcel.ExcelElement;
import Simpleexcel.File;
import Simpleexcel.Row;
import Simpleexcel.Sheet;

public class ExcelToXmlConversion {
	private HashMap<Row, Row> newConnectionMap = new HashMap<Row, Row>();
	private List<Row> removeOnlyList = new ArrayList<Row>();

	public void convert() throws IOException {
		ExcelToXmlConversion xmlToExcelConversion = new ExcelToXmlConversion();

		// convert Excel artefact to SimpleExcelmodel
		Optional<File> excelModel = xmlToExcelConversion.convertExcelToSimpleExcel();

		// pre-processing: disconnect the invalid row connections and save it as trg.xmi
		xmlToExcelConversion.preProcessing(excelModel, xmlToExcelConversion);
		storeSimpleExcelModelToXMI(excelModel);

		// call SYNC_APP
		// BasicConfigurator.configure();
		// SYNC_App sync = new SYNC_App(false);
		// sync.executeSync(sync);

		// convert simpleExcelmodel to Excel artefact
		// xmlToExcelConversionMainClass.convertSimpleExcelToExcel(excelElement);
	}

	/**
	 * This method processes existing excel model and make it TGG compatible.
	 * 
	 * @param xmlToExcelConversion
	 * 
	 * @param excelModel
	 */
	private void preProcessing(Optional<File> excelOptionalModel, ExcelToXmlConversion xmlToExcelConversion) {
		System.out.println("Starting pre-processing ... ");
		File file = null;
		if (excelOptionalModel.isPresent()) {
			file = excelOptionalModel.get();
			Sheet sheet = file.getSheet().size() > 0 ? file.getSheet().get(0) : null;
			if (sheet != null) {
				List<Row> rowList = sheet.getRowobject().size() > 0 ? sheet.getRowobject() : null;
				if (rowList != null) {
					Row row = rowList.get(0);
					while (row != null) {
						List<Cell> cellList = row.getCell();
						if (cellList.size() > 0) {
							Cell cell_1 = cellList.get(0);
							Cell cell_2 = cellList.get(1);
							Cell cell_3 = cellList.get(2);
							Cell cell_4 = cellList.get(3);

							if (cell_1 != null && cell_2 != null && cell_3 != null && cell_4 != null) {

								if (!(isOnlySpaceOrBlank(cell_1) && isOnlySpaceOrBlank(cell_2)
										&& isOnlySpaceOrBlank(cell_3) && isOnlySpaceOrBlank(cell_4))) {

									// check if Plugin-block-header : if yes, then remove connection to previous
									// row.
									if (xmlToExcelConversion.checkIfPluginHeader(cell_1, cell_2, cell_3, cell_4)) {
										if (row.getPrevRow() != null) {
											System.out.println("Row : " + cell_1.getText());
											row.setPrevRow(null);
										}
									}
								} else {// a row with space/tab cells only: remove this.
									removeRow(row, xmlToExcelConversion, sheet);
								}
							}
							row = row.getNextRow();
						} else {
							removeRow(row, xmlToExcelConversion, sheet);
							row = row.getNextRow();
						}
					}

				}
			}
			// create new connections and delete obsolete ones
			for (Map.Entry<Row, Row> entry : newConnectionMap.entrySet()) {
				Row prevRow = entry.getKey();
				Row nextRow = entry.getValue();
				Row currentRow = prevRow.getNextRow();
				prevRow.setNextRow(nextRow);
				for (Cell cell : currentRow.getCell()) {
					sheet.getCell().remove(cell);
				}
				currentRow.getCell().removeAll(currentRow.getCell());
				sheet.getRowobject().remove(currentRow);
			}

			// remove rows from removeOnlyList
			if (removeOnlyList != null) {
				for (Row removeRow : removeOnlyList) {
					System.out.println();
					removeRow.getPrevRow().setNextRow(null);
					for (Cell cell : removeRow.getCell()) {
						sheet.getCell().remove(cell);
					}
					removeRow.getCell().removeAll(removeRow.getCell());
					sheet.getRowobject().remove(removeRow);
				}
			}

		}

		System.out.println("pre-processing end...");
	}

	/**
	 * Section of code for checking and removing the row
	 * 
	 * @param row
	 * @param xmlToExcelConversion
	 * @param sheet
	 */
	private void removeRow(Row row, ExcelToXmlConversion xmlToExcelConversion, Sheet sheet) {
		Row prevRow = row.getPrevRow();
		Row nextRow = row.getNextRow();
		if (nextRow != null) {
			boolean ifPluginHeader = xmlToExcelConversion.checkIfPluginHeader(nextRow.getCell().get(0),
					nextRow.getCell().get(1), nextRow.getCell().get(2), nextRow.getCell().get(3));
			if (nextRow != null && !ifPluginHeader) {
				// add to map
				newConnectionMap.put(prevRow, nextRow);
			} else if (ifPluginHeader) {
				removeOnlyList.add(row);
			}
		} else {
			for (Cell cell : row.getCell()) {
				sheet.getCell().remove(cell);
			}
			row.getCell().removeAll(row.getCell());
			sheet.getRowobject().remove(row);
		}
	}

	/**
	 * Checks if a row is plugin block header
	 * 
	 * @param cell_1
	 * @param cell_2
	 * @param cell_3
	 * @param cell_4
	 * @return
	 */
	private boolean checkIfPluginHeader(Cell cell_1, Cell cell_2, Cell cell_3, Cell cell_4) {
		if ((cell_1 != null && cell_1.getText() != null && !cell_1.getText().equalsIgnoreCase(""))
				&& (cell_1 == null || cell_2.getText() == null || cell_2.getText().equalsIgnoreCase(""))
				&& (cell_3 == null || cell_2.getText() == null || cell_3.getText().equalsIgnoreCase(""))
				&& (cell_3 == null || cell_2.getText() == null || cell_4.getText().equalsIgnoreCase(""))) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if a strings contain only space(s), tab(s)
	 * 
	 * @param cell_1
	 * @return
	 */
	private boolean isOnlySpaceOrBlank(Cell cell_1) {
		if (cell_1.getText() != null) {
			if (!cell_1.getText().equals("")) {
				final String regex = "^\\s+$";
				final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
				final Matcher matcher = pattern.matcher(cell_1.getText());
				if (matcher.find()) {
					// System.out.println("Full match: " + matcher.group(0));
					return true;
				} else
					return false;
			} else {
				return true;
			}
		} else {
			return true;
		}
	}

	/**
	 * @return
	 *
	 */
	private Optional<File> convertExcelToSimpleExcel() {
		ExcelToOrFromSimpleExcel excelToSimpleExcel = new ExcelToOrFromSimpleExcel();
		return excelToSimpleExcel.convertExcelToSimpleExcel();
	}

	/**
	 * This method stores SimpleExcel Model to .xmi in an intermediate path before
	 * post processing
	 * 
	 * @param excelModel
	 * 
	 * @param excelArtefactAdapter
	 */
	public void storeSimpleExcelModelToXMI(Optional<File> model) {
		model.ifPresent(m -> {
			XMIArtefactAdapter<ExcelElement> xmiArtefactAdapter = new XMIArtefactAdapter<ExcelElement>(
					Paths.get(CONSTANTS.SIMPLE_EXCEL_XMI_PATH));
			xmiArtefactAdapter.setModel(m);
			xmiArtefactAdapter.unparse();
		});
		System.out.println("Parsing completed ...");
	}
}
