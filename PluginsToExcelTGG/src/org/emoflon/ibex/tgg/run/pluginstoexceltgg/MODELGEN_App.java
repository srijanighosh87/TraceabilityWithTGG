package org.emoflon.ibex.tgg.run.pluginstoexceltgg;

import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.emoflon.ibex.tgg.operational.csp.constraints.factories.UserDefinedRuntimeTGGAttrConstraintFactory;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.gen.MODELGEN;
import org.emoflon.ibex.tgg.operational.strategies.gen.MODELGENStopCriterion;
import org.emoflon.ibex.tgg.runtime.engine.DemoclesTGGEngine;

import Simpletree.File;
import Simpletree.Folder;
import Simpletree.Node;
import Simpletree.SimpletreePackage;
import Simpletree.Text;
import Simpletree.TreeElement;
import Simpletree.impl.FolderImpl;

public class MODELGEN_App extends MODELGEN {

	public MODELGEN_App() throws IOException {
		super(createIbexOptions());
		registerBlackInterpreter(new DemoclesTGGEngine());
	}

	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();

		MODELGEN_App generator = new MODELGEN_App();

		MODELGENStopCriterion stop = new MODELGENStopCriterion(generator.getTGG());
		// stop.setTimeOutInMS(1000);
		stop.setMaxRuleCount("XMLFolderToExcelFileRelation", 1);
		stop.setMaxRuleCount("CreateSheetStructure", 1);
		stop.setMaxRuleCount("PluginFolderToBlockHeaderRowRelation", 100);

		stop.setMaxRuleCount("ExtensionNodeToNewRowUnderBlockHeaderRelation", 90);
		stop.setMaxRuleCount("ExtensionNodeToNewRowRelation", 8000);

		// stop.setMaxSrcCount(20);
		// stop.setMaxElementCount(20);
		// stop.setTimeOutInMS(500);
		generator.setStopCriterion(stop);

		logger.info("Starting MODELGEN");
		long tic = System.currentTimeMillis();
		generator.run();
		long toc = System.currentTimeMillis();
		logger.info("Completed MODELGEN in: " + (toc - tic) + " ms");

		generator.saveModels();
		generator.terminate();
		
		System.out.println(getExtensionNumbers());
	}

	protected void registerUserMetamodels() throws IOException {
		_RegistrationHelper.registerMetamodels(rs, this);

		// Register correspondence metamodel last
		loadAndRegisterMetamodel(options.projectPath() + "/model/" + options.projectPath() + ".ecore");
	}

	private static IbexOptions createIbexOptions() {
		IbexOptions options = new IbexOptions();
		options.projectName("PluginsToExcelTGG");
		options.projectPath("PluginsToExcelTGG");
		options.debug(false);
		options.userDefinedConstraints(new UserDefinedRuntimeTGGAttrConstraintFactory());
		return options;
	}
	
	/**
	 * @param string
	 * @return
	 */
	private static int getExtensionNumbers() {
		int extNumber = 0;
		TreeElement root = readSimpleTreeXMIModel("./instances/src.xmi");

		if (root instanceof FolderImpl) {
			Folder rootFolder = (Folder) root;
			//System.out.println("WORKSPACE :: " + rootFolder);
			if (rootFolder.getSubFolder().size() > 0) {
				for (Folder project : rootFolder.getSubFolder()) {
					//System.out.println("PROJECT :: " + project);
					if (project.getFile().size() > 0) {
						// System.out.println("NO OF XML :: " + project.getFile().size());
						if (project.getFile().size() == 1) {
							File pluginXml = project.getFile().get(0);
							// System.out.println(" XML :: " + pluginXml.getName());
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

		System.out.println(extNumber);

		return extNumber;
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
		}
		return null;
	}
}
