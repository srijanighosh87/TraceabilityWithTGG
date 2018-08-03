package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emoflon.ibex.tgg.run.pluginstoexceltgg.SYNC_App;

import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset.Entry;
import com.kaleidoscope.core.auxiliary.xmi.artefactadapter.XMIArtefactAdapter;

import Simpleexcel.Cell;
import Simpleexcel.ExcelElement;
import Simpleexcel.File;
import Simpleexcel.Row;
import Simpleexcel.Sheet;
import Simpletree.Attribute;
import Simpletree.Folder;
import Simpletree.Node;
import Simpletree.SimpletreePackage;
import Simpletree.Text;
import Simpletree.TreeElement;
import Simpletree.impl.FolderImpl;

public class ExcelToXmlConversion {
	private HashMap<Row, Row> newConnectionMap = new HashMap<Row, Row>();
	private List<Row> removeOnlyList = new ArrayList<Row>();
	private String workspacePath = "";
	private TreeElement simpleTreeModel = null;

	static BufferedWriter bw = null;
	static FileWriter fw = null;
	private static final String logFIle = "C:\\Users\\Srijani\\Desktop\\log.txt";
	static java.io.File file = new java.io.File(logFIle);

	public void convert(String excelPath, String workspacePath) throws IOException {
		this.workspacePath = workspacePath;

		// convert Excel artefact to SimpleExcelmodel
		Optional<ExcelElement> excelModel = this.convertExcelToSimpleExcel(excelPath);

		// pre-processing: disconnect the invalid row connections and save it as trg.xmi
		this.preProcessing(excelModel, this);
		storeSimpleExcelModelToXMI(excelModel);

		// call SYNC_APP
		BasicConfigurator.configure();
		excelModel.ifPresent(excel -> {
			try {
				SYNC_App sync = new SYNC_App(false);
				sync.executeSync(sync);
				simpleTreeModel = (TreeElement) sync.getSourceResource().getContents().get(0);
			} catch (IOException e) {
				e.printStackTrace();
			}

		});

		// post processing on SimpleTree model
		simpleTreeModel = this.readSimpleTreeXMIModel();
		this.postProcessing(simpleTreeModel);
		storeSimpleTreeModelToXMI(Optional.of(simpleTreeModel));

		// read xmi file and convert simpleExcelmodel to Excel artefact
		this.convertSimpleTreeToWorkspace(simpleTreeModel);
	}

	/**
	 * post processing on generated XML
	 * 
	 * @param simpleTreeModel
	 */
	private void postProcessing(TreeElement simpleTreeModel) {

		List<String> extensionList = new ArrayList<String>();
		List<String> extensionPointList = new ArrayList<String>();

		// replace #
		if (simpleTreeModel != null) {
			if (simpleTreeModel instanceof FolderImpl) {
				Folder workspaceFolder = (Folder) simpleTreeModel;
				for (Folder projectFolder : workspaceFolder.getSubFolder()) {
					for (Simpletree.File pluginxmlFile : projectFolder.getFile()) {
						Node pluginNode = ((Node) pluginxmlFile.getRootNode());
						// replace # to . in extension or extension-point
						for (Text child : pluginNode.getChildren()) {
							if (child.getName().equalsIgnoreCase("extension")
									|| child.getName().equalsIgnoreCase("extension-point")) {
								if (child.getName().equalsIgnoreCase("extension")) {
									for (Attribute attr : ((Node) child).getAttribute()) {
										if (attr.getName().equalsIgnoreCase("point")) {
											if (attr.getValue().contains("#")) {
												String oldVal = attr.getValue();
												String newVal = oldVal.replace("#", ".");
												attr.setValue(newVal);
												extensionList.add(newVal);
											}
										}
									}
								}
								if (child.getName().equalsIgnoreCase("extension-point")) {
									for (Attribute attr : ((Node) child).getAttribute()) {
										if (attr.getName().equalsIgnoreCase("id")) {
											if (attr.getValue().contains("#")) {
												String oldVal = attr.getValue();
												String newVal = oldVal.replace("#", ".");
												attr.setValue(newVal);
												extensionPointList.add(newVal);
											}
										}
									}
								}

							}

						}
					}
				}
			}
		}

		// group by name and combine same projects
		if (simpleTreeModel != null) {
			if (simpleTreeModel instanceof FolderImpl) {
				Folder workspaceFolder = (Folder) simpleTreeModel;
				Map<String, List<Folder>> projectMap = workspaceFolder.getSubFolder().stream()
						.collect(Collectors.groupingBy(folder -> folder.getName()));
				for (Map.Entry<String, List<Folder>> entry : projectMap.entrySet()) {
					System.out.println(entry.getKey());
					System.out.println(entry.getValue());
					if (entry.getValue().size() > 1) {
						List<Text> extList = new ArrayList<Text>();
						List<Text> extPtList = new ArrayList<Text>();
						int count = 0;
						for (Folder project : entry.getValue()) {

							for (Text child : ((Node) project.getFile().get(0).getRootNode()).getChildren()) {
								if (child.getName().equalsIgnoreCase("extension")) {
									extList.add(child);
									if (count != 0) {
										project.getParentFolder().getSubFolder().remove(project);
									}
								}

								if (child.getName().equalsIgnoreCase("extension-point")) {
									extPtList.add(child);
									if (count != 0) {
										project.getParentFolder().getSubFolder().remove(project);
									}
								}

								count++;
							}
						}

						((Node) entry.getValue().get(0).getFile().get(0).getRootNode()).getChildren().addAll(extList);
						((Node) entry.getValue().get(0).getFile().get(0).getRootNode()).getChildren().addAll(extPtList);

					}
					System.out.println();
				}
			}
		}

		// remove duplicate extension points
		if (simpleTreeModel != null) {
			if (simpleTreeModel instanceof FolderImpl) {
				Folder workspaceFolder = (Folder) simpleTreeModel;
				for (Folder project : workspaceFolder.getSubFolder()) {
					Node pluginNode = (Node) project.getFile().get(0).getRootNode();

					Map<Object, List<Text>> extensionMap = pluginNode.getChildren().stream()
							.collect(Collectors.groupingBy(extension -> extension.getName()));
					int count = 0;
					for (java.util.Map.Entry<Object, List<Text>> entry : extensionMap.entrySet()) {
						System.out.println(entry.getKey());
						System.out.println(entry.getValue());
						if(entry.getKey().toString().equalsIgnoreCase("extension-point")) {
							if (entry.getValue().size() > 1) {
								for (Text extension : entry.getValue()) {
									if (count > 0) {
										pluginNode.getChildren().remove(extension);
									}
									count++;
								}
							}
						}

					}

				}
			}
		}

	}

	/**
	 * reads xmi for SimpleTree and stores in model
	 * 
	 * @return
	 */
	public TreeElement readSimpleTreeXMIModel() {
		SimpletreePackage sp = SimpletreePackage.eINSTANCE;
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource resource = rs.getResource(URI.createURI(CONSTANTS.SIMPLE_TREE_XMI_PATH), true);
		try {
			TreeElement obj = (TreeElement) resource.getContents().get(0);
			return obj;
		} catch (Exception e) {
			System.out.println("ERROR :: " + e);
		}
		return null;
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
			file = (Simpleexcel.File) excelOptionalModel.get();
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
						if (checkPreviousOrNextRow(currentRow, "prev") && checkPreviousOrNextRow(currentRow, "next")) {
							removeOnlyList.add(currentRow);
						}
					}
				}
			}

			// replace pluginname.extension to pluginname#extension because TGG concat does
			// not support separator multiple times in string
			if (sheet != null) {
				for (Row rowObject : sheet.getRowobject()) {
					if (rowObject.getCell().size() == 4) {
						Cell ext_cell_1 = rowObject.getCell().get(0);
						Cell ext_cell_2 = rowObject.getCell().get(1);
						Cell ext_cell_3 = rowObject.getCell().get(2);
						Cell ext_cell_4 = rowObject.getCell().get(3);
						if (ext_cell_1 != null && ext_cell_2 != null && ext_cell_3 != null && ext_cell_4 != null) {
							// check if contains any empty/blank cell
							if (!(isOnlySpaceOrBlank(ext_cell_1) && isOnlySpaceOrBlank(ext_cell_2)
									&& isOnlySpaceOrBlank(ext_cell_3) && isOnlySpaceOrBlank(ext_cell_4))) {
								// check if the row is an extension row
								if (xmlToExcelConversion.checkIfExtensionRow(ext_cell_1, ext_cell_2, ext_cell_3,
										ext_cell_4)) {
									StringBuilder strb = new StringBuilder(ext_cell_2.getText());
									int index = strb.lastIndexOf(".");
									strb.replace(index, ".".length() + index, "#");
									ext_cell_2.setText(strb.toString());
								}
							}
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
	 * @param cell_1
	 * @param cell_2
	 * @param cell_3
	 * @param cell_4
	 * @return
	 */
	private boolean checkIfExtensionRow(Cell cell_1, Cell cell_2, Cell cell_3, Cell cell_4) {
		if ((cell_1 == null || cell_1.getText() == null || cell_1.getText().equalsIgnoreCase(""))
				&& (cell_2 != null && cell_2.getText() != null && !cell_2.getText().equalsIgnoreCase(""))) {
			return true;
		}
		return false;
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
				&& (cell_2 == null || cell_2.getText() == null || cell_2.getText().equalsIgnoreCase(""))
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
	 * 
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

	/**
	 * This method stores SimpleExcel Model to .xmi in an intermediate path before
	 * post processing
	 * 
	 * @param model
	 */
	public void storeSimpleTreeModelToXMI(Optional<TreeElement> model) {
		model.ifPresent(m -> {
			XMIArtefactAdapter<TreeElement> xmiArtefactAdapter = new XMIArtefactAdapter<TreeElement>(
					Paths.get(CONSTANTS.SIMPLE_TREE_XMI_PATH));
			xmiArtefactAdapter.setModel(m);
			xmiArtefactAdapter.unparse();
		});
		System.out.println("Parsing completed ...");
	}
}
