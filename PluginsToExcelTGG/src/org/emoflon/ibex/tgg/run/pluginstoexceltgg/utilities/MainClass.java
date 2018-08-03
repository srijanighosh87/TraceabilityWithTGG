package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emoflon.ibex.tgg.operational.strategies.gen.MODELGENStopCriterion;
import org.emoflon.ibex.tgg.run.pluginstoexceltgg.MODELGEN_App;
import org.emoflon.ibex.tgg.run.pluginstoexceltgg.SYNC_App;


import com.kaleidoscope.core.auxiliary.simpleexcel.artefactadapter.ExcelArtefactAdapter;

import Simpleexcel.ExcelElement;
import Simpletree.SimpletreePackage;
import Simpletree.TreeElement;

public class MainClass {
	public static boolean CONVERSION_DIRECTION;

	public static void main(String[] args) {

		//generateModel(1, 1);
		
		System.out.println("New project");
		Logger.getRootLogger().setLevel(Level.DEBUG);

		// true: XML_TO_EXCEL; false:EXCEL_TO_XML
		CONVERSION_DIRECTION = false;

		if (CONVERSION_DIRECTION) {
			try {
				System.out.println("Transforming Plugin Workspace to EXCEL");
				
				/*new XmlToExcelConversion().convert("test",
						"C:\\Users\\Srijani\\Desktop\\final test\\abc.xlsx");*/
			
				new XmlToExcelConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\forward\\ibex",
						"C:\\Users\\Srijani\\Desktop\\final test\\forward\\ibex\\Relationship.xlsx");
				
				/*new XmlToExcelConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\forward\\acceleo-master\\plugins",
						"C:\\Users\\Srijani\\Desktop\\final test\\forward\\acceleo-master\\plugins\\Relationship.xlsx");
				
				new XmlToExcelConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\forward\\eclipse.jdt.core-master",
						"C:\\Users\\Srijani\\Desktop\\final test\\forward\\eclipse.jdt.core-master\\Relationship.xlsx");
				
				new XmlToExcelConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\forward\\emf-master-plugins",
						"C:\\Users\\Srijani\\Desktop\\final test\\forward\\emf-master-plugins\\Relationship.xlsx");
				
				new XmlToExcelConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\forward\\test",
						"C:\\Users\\Srijani\\Desktop\\final test\\forward\\test\\Relationship.xlsx");*/
				
			} catch (Exception e) {
				System.out.println("Some error  occurred ..." + e);
			}
		} else {
			// int execCount = 0;
			// while(execCount<10) {
			try {
				System.out.println("Transforming EXCEL to Plugin Workspace");

				
		/*		new ExcelToXmlConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\abc.xlsx", 
				  "C:\\Users\\Srijani\\Desktop\\final test\\temp");
				*/
				new ExcelToXmlConversion().convert(
						"C:\\Users\\Srijani\\Desktop\\final test\\backward\\ibex\\Relationship.xlsx",
						"C:\\Users\\Srijani\\Desktop\\final test\\backward\\ibex");
				
				/*new ExcelToXmlConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\backward\\acceleo-master\\plugins\\Relationship.xlsx", 
						"C:\\Users\\Srijani\\Desktop\\final test\\backward\\acceleo-master\\plugins");*/
				
				/*	new ExcelToXmlConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\backward\\eclipse.jdt.core-master\\Relationship.xlsx", 
						"C:\\Users\\Srijani\\Desktop\\final test\\backward\\eclipse.jdt.core-master");
				
					new ExcelToXmlConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\backward\\emf-master-plugins\\Relationship.xlsx", 
						"C:\\Users\\Srijani\\Desktop\\final test\\backward\\emf-master-plugins");
				
				
				new ExcelToXmlConversion().convert("C:\\Users\\Srijani\\Desktop\\final test\\backward\\test10\\Relationship.xlsx", 
						"C:\\Users\\Srijani\\Desktop\\final test\\backward\\test10");*/
				
				
			} catch (IOException e) {
				System.out.println("Some error occurred ...");
			}

			System.out.println(
					"==============================================================================================");
			// }

		}
	}
	
	/**
	 * reads xmi for Excel and stores in model
	 * 
	 * @return
	 */
	private static ExcelElement readSimpleExcelXMIModel(String path) {
		SimpletreePackage sp = SimpletreePackage.eINSTANCE;
		ResourceSet rs = new ResourceSetImpl();
		rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource resource = rs.getResource(URI.createURI(path), true);
		try {
			ExcelElement obj = (ExcelElement) resource.getContents().get(0);
			return obj;
		} catch (Exception e) {
			System.out.println("ERROR :: " + e);
		}
		return null;
	}

	private static void generateModel(int noOfWorkspace, int noOfProjects) {
		BasicConfigurator.configure();

		MODELGEN_App generator;
		try {
			generator = new MODELGEN_App();

			MODELGENStopCriterion stop = new MODELGENStopCriterion(generator.getTGG());

			stop.setMaxRuleCount("XMLFolderToExcelFileRelation", 1);
			stop.setMaxRuleCount("CreateSheetStructure", 1);
			stop.setMaxRuleCount("PluginFolderToBlockHeaderRowRelation", 2);
			stop.setMaxRuleCount("ExtensionNodeToNewRowUnderBlockHeaderRelation", 2);
			stop.setMaxRuleCount("ExtensionNodeToNewRowRelation", 1);
			
			stop.setMaxRuleCount("MultipleExtPoint_ExtensionNodeToNewRowRelation", 1);
			stop.setMaxRuleCount("MultipleExtPoint_ExtensionNodeToNewRowUnderBlockHeaderRelation", 1);
			stop.setMaxRuleCount("NoNewExtPoint_ExtensionNodeToNewRowRelation", 1);
			stop.setMaxRuleCount("NoNewExtPoint_ExtensionNodeToNewRowUnderBlockHeaderRelation", 1);


			stop.setMaxElementCount(1000);


			generator.setStopCriterion(stop);

			System.out.println("Starting MODELGEN");
			long tic = System.currentTimeMillis();
			generator.run();
			long toc = System.currentTimeMillis();
			System.out.println("Completed MODELGEN in: " + (toc - tic) + " ms");

			generator.saveModels();
			generator.terminate();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
