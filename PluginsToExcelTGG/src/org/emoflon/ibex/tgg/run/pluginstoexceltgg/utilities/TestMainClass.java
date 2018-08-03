package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emoflon.ibex.tgg.operational.strategies.gen.MODELGENStopCriterion;
import org.emoflon.ibex.tgg.run.pluginstoexceltgg.MODELGEN_App;
import org.emoflon.ibex.tgg.run.pluginstoexceltgg.SYNC_App;

import Simpletree.File;
import Simpletree.Folder;
import Simpletree.Node;
import Simpletree.SimpletreePackage;
import Simpletree.Text;
import Simpletree.TreeElement;
import Simpletree.impl.FolderImpl;

public class TestMainClass {

	// Delimiter used in CSV file
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	// CSV file header
	private static final String FILE_HEADER = "Timestamp,NoOfWorkspace,NoOfProjects,ExtensionSize,Forward Sync TimeRequired(ms),Backward Sync TimeRequired(ms)";

	private static String fileName = "C:\\Users\\Srijani\\Desktop\\Report.csv";

	private static final String logFIle = "C:\\Users\\Srijani\\Desktop\\log.txt";

	static BufferedWriter bw = null;
	static FileWriter fw = null;
	static java.io.File file = new java.io.File(logFIle);

	public static void main(String[] args) {
		initializeLog();
		addheaderToCSV(fileName);

		System.out.println("STARTING");
		WriteToLog("STARTING TEST ...");

		// test for mdoel size = 10
		int noOfWorkspace = 1;
		int noOfProjects = 5;

		int count = 0;
		int sizeOfModel = 0;
		while (count < 1) {
			sizeOfModel = 0;

			while (sizeOfModel < 9 || sizeOfModel > 11) {
				// generate model
				generateModel(noOfWorkspace, noOfProjects, 100);

				// calculate number of extensions
				sizeOfModel = getExtensionNumbers();

				System.out.println("Model generated, size: " + sizeOfModel);
				WriteToLog("Model generated, size: " + sizeOfModel);

			}

			// do sync
			long forwardSyncTime = sync(true);
			WriteToLog("Forward sync completed");

			long backwardSyncTime = sync(false);
			WriteToLog("Backward sync completed");

			// print time
			addtoCSV(10, count, noOfWorkspace, noOfProjects, sizeOfModel, forwardSyncTime, backwardSyncTime);

			// repeat
			count++;
		}

		System.out.println("Process completed for model size 10");
		WriteToLog(
				"Process completed for model size 10 \n ==============================================================================");

		// test for mdoel size = 100
		count = 0;
		sizeOfModel = 0;
		while (count < 10) {
			while (sizeOfModel < 90 || sizeOfModel > 110) {
				// generate model
				generateModel(noOfWorkspace, noOfProjects, 1000);

				// calculate number of extensions
				sizeOfModel = getExtensionNumbers();

				System.out.println(sizeOfModel);
				WriteToLog("Model generated, size: " + sizeOfModel);
			}

			// do sync
			// do sync
			long forwardSyncTime = sync(true);
			WriteToLog("Forward sync completed");

			long backwardSyncTime = sync(false);
			WriteToLog("Backward sync completed");

			// print time
			addtoCSV(100, count, noOfWorkspace, noOfProjects, sizeOfModel, forwardSyncTime, backwardSyncTime);

			// repeat
			count++;
		}

		System.out.println("Process completed for model size 100");
		WriteToLog(
				"Process completed for model size 100 \\n ==============================================================================");

		// test for mdoel size = 1000
		noOfProjects = 10;
		count = 0;
		sizeOfModel = 0;
		while (count < 10) {
			while (sizeOfModel < 990 || sizeOfModel > 1010) {
				// generate model
				generateModel(noOfWorkspace, noOfProjects, 10000);

				// calculate number of extensions
				sizeOfModel = getExtensionNumbers();

				System.out.println(sizeOfModel);
				WriteToLog("Model generated, size: " + sizeOfModel);
			}

			// do sync
			// do sync
			long forwardSyncTime = sync(true);
			WriteToLog("Forward sync completed");

			long backwardSyncTime = sync(false);
			WriteToLog("Backward sync completed");

			// print time
			addtoCSV(100, count, noOfWorkspace, noOfProjects, sizeOfModel, forwardSyncTime, backwardSyncTime);

			// repeat
			count++;
		}

		System.out.println("Process completed for model size 1000");
		WriteToLog(
				"Process completed for model size 1000 \\n ==============================================================================");

		// test for mdoel size = 10000
		/*
		 * noOfProjects = 100; count = 0; sizeOfModel = 0; while (count < 10) { while
		 * (sizeOfModel < 9990 || sizeOfModel > 10010) { // generate model
		 * generateModel(noOfWorkspace, noOfProjects, 90, 8000, 60, 1900);
		 * 
		 * // calculate number of extensions sizeOfModel = getExtensionNumbers();
		 * 
		 * System.out.println(sizeOfModel); }
		 * 
		 * // do sync long syncTime = sync(true);
		 * 
		 * // print time addtoCSV(10000, count, noOfWorkspace, noOfProjects,
		 * sizeOfModel, forwardSyncTime, backwardSyncTime);
		 * 
		 * // repeat count++; }
		 * 
		 * System.out.println("Process completed for model size 10000");
		 */

	}

	private static int getExtensionNumbers() {
		int extNumber = 0;
		TreeElement root = readSimpleTreeXMIModel("./instances/src.xmi");

		if (root instanceof FolderImpl) {
			Folder rootFolder = (Folder) root;
			if (rootFolder.getSubFolder().size() > 0) {
				for (Folder project : rootFolder.getSubFolder()) {
					if (project.getFile().size() > 0) {
						if (project.getFile().size() == 1) {
							File pluginXml = project.getFile().get(0);
							if (pluginXml.getRootNode().getName().equalsIgnoreCase("plugin")) {
								if (pluginXml.getRootNode() instanceof Node) {
									Node pluginNode = (Node) pluginXml.getRootNode();
									if (pluginNode.getChildren().size() > 0) {
										for (Text children : pluginNode.getChildren()) {
											if (children.getName().equalsIgnoreCase("extension")
													|| children.getName().equalsIgnoreCase("extension-point")) {
												extNumber++;
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

		return extNumber;
	}
	private static void initializeLog() {
		// if file doesnt exists, then create it
		if (!file.exists()) {
			try {
				file.createNewFile();

				fw = new FileWriter(file.getAbsoluteFile(), true);
				bw = new BufferedWriter(fw);

			} catch (IOException e) {
				e.printStackTrace();
			} finally {

				try {
					if (bw != null)
						bw.close();
					if (fw != null)
						fw.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	/**
	 * writes to local log
	 * 
	 * @param string
	 */
	private static void WriteToLog(String string) {
		try {
			fw = new FileWriter(file.getAbsoluteFile(), true);
			bw = new BufferedWriter(fw);

			bw.write(LocalDateTime.now() + "	" +string);
			bw.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bw != null)
					bw.close();
				if (fw != null)
					fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * @param time
	 * @param modelSize
	 * @param loopNumber
	 * @param noOfWorkspace
	 * @param noOfProjects
	 * @param extensionSize
	 * @param forwardSyncTime
	 * @param backwardSyncTime
	 */
	private static void addtoCSV(int modelSize, int loopNumber, int noOfWorkspace, int noOfProjects, int extensionSize,
			long forwardSyncTime, long backwardSyncTime) {

		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(fileName, true);

			// add data
			fileWriter.append(LocalDateTime.now() + "");
			fileWriter.append(COMMA_DELIMITER);
			fileWriter.append(noOfWorkspace + "");
			fileWriter.append(COMMA_DELIMITER);
			fileWriter.append(noOfProjects + "");
			fileWriter.append(COMMA_DELIMITER);
			fileWriter.append(extensionSize + "");
			fileWriter.append(COMMA_DELIMITER);
			fileWriter.append(forwardSyncTime + "");
			fileWriter.append(COMMA_DELIMITER);
			fileWriter.append(backwardSyncTime + "");
			fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (IOException e) {
			WriteToLog("exception ::" + e.getMessage());
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();

			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				WriteToLog("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}
		}

	}

	/**
	 * @param fileName
	 */
	private static void addheaderToCSV(String fileName) {
		FileWriter fileWriter = null;

		try {
			fileWriter = new FileWriter(fileName, true);

			// Write the CSV file header
			fileWriter.append(FILE_HEADER.toString());

			// Add a new line separator after the header
			fileWriter.append(NEW_LINE_SEPARATOR);

		} catch (IOException e) {
			e.printStackTrace();
			WriteToLog("exception ::" + e.getMessage());
		} finally {
			try {
				fileWriter.flush();
				fileWriter.close();

			} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				WriteToLog("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param time
	 * @param j
	 * @param i
	 * @param noOfProjects
	 * @param j
	 * @param i
	 */
	private static void generateModel(int noOfWorkspace, int noOfProjects, int m) {
		BasicConfigurator.configure();

		MODELGEN_App generator;
		try {
			generator = new MODELGEN_App();

			MODELGENStopCriterion stop = new MODELGENStopCriterion(generator.getTGG());

			stop.setMaxRuleCount("XMLFolderToExcelFileRelation", noOfWorkspace);
			stop.setMaxRuleCount("CreateSheetStructure", noOfWorkspace);
			stop.setMaxRuleCount("PluginFolderToBlockHeaderRowRelation", noOfProjects);

			stop.setMaxSrcCount(m);

			// stop.setMaxRuleCount("ExtensionNodeToNewRowUnderBlockHeaderRelation", i);
			// stop.setMaxRuleCount("ExtensionNodeToNewRowRelation", j);

			generator.setStopCriterion(stop);

			System.out.println("Starting MODELGEN");
			WriteToLog("Starting MODELGEN");
			long tic = System.currentTimeMillis();
			generator.run();
			long toc = System.currentTimeMillis();
			System.out.println("Completed MODELGEN in: " + (toc - tic) + " ms");
			WriteToLog("Completed MODELGEN in: " + (toc - tic) + " ms");

			generator.saveModels();
			generator.terminate();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	/**
	 * 
	 * @return
	 */
	
	/**
	 * @return
	 * 
	 */
	private static long sync(boolean fwd) {
		BasicConfigurator.configure();
		SYNC_App sync;
		try {
			sync = new SYNC_App(fwd);
			long time = sync.executeSync(sync);

			return time;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;

	}

	/**
	 * reads xmi for SimpleTree and stores in model
	 * 
	 * @return
	 */
	private static TreeElement readSimpleTreeXMIModel(String path) {
		SimpletreePackage sp = SimpletreePackage.eINSTANCE;
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource resource = rs.getResource(URI.createURI(path), true);
		try {
			TreeElement obj = (TreeElement) resource.getContents().get(0);
			return obj;
		} catch (Exception e) {
			System.out.println("ERROR :: " + e);
			WriteToLog("ERROR :: " + e);
		}
		return null;
	}

}
