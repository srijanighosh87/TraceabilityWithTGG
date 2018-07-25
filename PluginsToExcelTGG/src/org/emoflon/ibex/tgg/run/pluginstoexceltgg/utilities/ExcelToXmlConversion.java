package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import Simpletree.TreeElement;

public class ExcelToXmlConversion {
	private HashMap<Row, Row> newConnectionMap = new HashMap<Row, Row>();
	private List<Row> removeOnlyList = new ArrayList<Row>();
	private String workspacePath = "";
	private TreeElement simpleTreeModel = null;

	public void convert(String excelPath, String workspacePath) throws IOException {
		this.workspacePath= workspacePath;
		
		// convert Excel artefact to SimpleExcelmodel
		Optional<ExcelElement> excelModel = this.convertExcelToSimpleExcel(excelPath);

		// pre-processing: disconnect the invalid row connections and save it as trg.xmi
		this.preProcessing(excelModel, this);
		storeSimpleExcelModelToXMI(excelModel);

		// call SYNC_APP
		BasicConfigurator.configure();
		excelModel.ifPresent(excel ->{
			try {
				SYNC_App sync = new SYNC_App(false);
				sync.executeSync(sync);
				
				//set model in local variable
				simpleTreeModel = (TreeElement) sync.getSourceResource().getContents().get(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		});

		// read xmi file and convert simpleExcelmodel to Excel artefact
		this.convertSimpleTreeToWorkspace(simpleTreeModel);
	}

	/**
	 * Method for converting SimpleTree model to the entire workspace
	 * 
	 * @param simpleTreeModel
	 */
	private void convertSimpleTreeToWorkspace(TreeElement simpleTreeModel) {

		XmlToOrFromSimpleTree xmlFromSimpleTree = new XmlToOrFromSimpleTree();
		xmlFromSimpleTree.convertSimpleTreeToXml(simpleTreeModel, workspacePath);

	}

/*	*//**
	 * Reads src.xmi and returns the model
	 * 
	 * @return
	 *//*
	private TreeElement readXMIModel() {
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource resource = rs.getResource(URI.createURI(CONSTANTS.SIMPLE_TREE_XMI_PATH), true);
		TreeElement obj = (TreeElement) resource.getContents().get(0);

		return obj;
	}
*/
	/**
	 * This method processes existing excel model and make it TGG compatible.
	 * 
	 * @param xmlToExcelConversion
	 * 
	 * @param excelModel
	 */
	private void preProcessing(Optional<ExcelElement> excelOptionalModel, ExcelToXmlConversion xmlToExcelConversion) {
		System.out.println("Starting pre-processing ... ");
		File file = null;
		if (excelOptionalModel.isPresent()) {
			file = (Simpleexcel.File)excelOptionalModel.get();
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
							// if cells null
							if (cell_1 != null && cell_2 != null && cell_3 != null && cell_4 != null) {
								// if the row has only blank/empty cells
								if (!(isOnlySpaceOrBlank(cell_1) && isOnlySpaceOrBlank(cell_2)
										&& isOnlySpaceOrBlank(cell_3) && isOnlySpaceOrBlank(cell_4))) {

									// check if Plugin-block-header : if yes, then remove connection to previous
									// row.
									if (xmlToExcelConversion.checkIfPluginHeader(cell_1, cell_2, cell_3, cell_4)) {

										if (row.getPrevRow() != null) {
											// System.out.println("Row : " + cell_1.getText());
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
			removefromList(sheet);

			// check all rows for plugin header with no extension
			if (sheet != null) {
				removeOnlyList.removeAll(removeOnlyList);
				List<Row> rowList = sheet.getRowobject().size() > 0 ? sheet.getRowobject() : null;
				if (rowList != null) {
					for (int rowIndex = 1; rowIndex < rowList.size(); rowIndex++) {
						Row currentRow = rowList.get(rowIndex);
						/*
						 * System.out.println(currentRow.getCell().get(0).getText() + " , " +
						 * currentRow.getCell().get(1).getText() + " :::: " + "prev : " +
						 * currentRow.getPrevRow() + " :::: next : " + currentRow.getNextRow());
						 */
						if (checkPreviousOrNextRow(currentRow, "prev") && checkPreviousOrNextRow(currentRow, "next")) {
							removeOnlyList.add(currentRow);
						}
					}
				}
			}

			removefromList(sheet);

		}

		System.out.println("pre-processing end...");
	}

	/**
	 * @param currentRow
	 * @param string
	 * @return
	 */
	private boolean checkPreviousOrNextRow(Row currentRow, String string) {
		if (string.equalsIgnoreCase("prev")) {
			if (currentRow.getPrevRow() == null) {
				return true;
			} else {
				Row prevRow = currentRow.getPrevRow();
				if (prevRow.getCell().size() == 0 && prevRow.getCell().size() < 4) {
					return true;
				} else {
					Cell cell_1 = prevRow.getCell().get(0);
					Cell cell_2 = prevRow.getCell().get(1);
					Cell cell_3 = prevRow.getCell().get(2);
					Cell cell_4 = prevRow.getCell().get(3);
					if (cell_1.getText() == null && cell_2.getText() == null && cell_3.getText() == null
							&& cell_4.getText() == null) {
						return true;
					} else
						return false;
				}
			}
		}

		if (string.equalsIgnoreCase("next")) {
			if (currentRow.getNextRow() == null) {
				return true;
			} else {
				Row nextRow = currentRow.getNextRow();
				if (nextRow.getCell().size() == 0 && nextRow.getCell().size() < 4) {
					return true;
				} else {
					Cell cell_1 = nextRow.getCell().get(0);
					Cell cell_2 = nextRow.getCell().get(1);
					Cell cell_3 = nextRow.getCell().get(2);
					Cell cell_4 = nextRow.getCell().get(3);
					if (cell_1.getText() == null && cell_2.getText() == null && cell_3.getText() == null
							&& cell_4.getText() == null) {
						return true;
					} else
						return false;
				}
			}
		}

		return false;
	}

	/**
	 * @param sheet
	 */
	private void removefromList(Sheet sheet) {
		if (removeOnlyList != null) {
			for (Row removeRow : removeOnlyList) {
				if (removeRow.getPrevRow() != null) {
					removeRow.getPrevRow().setNextRow(null);
				}
				for (Cell cell : removeRow.getCell()) {
					sheet.getCell().remove(cell);
				}
				removeRow.getCell().removeAll(removeRow.getCell());
				sheet.getRowobject().remove(removeRow);

			}
		}
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
	private Optional<ExcelElement> convertExcelToSimpleExcel(String excelPath) {
		ExcelToOrFromSimpleExcel excelToSimpleExcel = new ExcelToOrFromSimpleExcel();
		return excelToSimpleExcel.convertExcelToSimpleExcel(excelPath);
	}


	/**
	 * This method stores SimpleExcel Model to .xmi in an intermediate path before
	 * post processing
	 * @param model
	 */
	public void storeSimpleExcelModelToXMI(Optional<ExcelElement> model) {
		model.ifPresent(m -> {
			XMIArtefactAdapter<ExcelElement> xmiArtefactAdapter = new XMIArtefactAdapter<ExcelElement>(
					Paths.get(CONSTANTS.SIMPLE_EXCEL_XMI_PATH));
			xmiArtefactAdapter.setModel(m);
			xmiArtefactAdapter.unparse();
		});
		System.out.println("Parsing completed ...");
	}
}
