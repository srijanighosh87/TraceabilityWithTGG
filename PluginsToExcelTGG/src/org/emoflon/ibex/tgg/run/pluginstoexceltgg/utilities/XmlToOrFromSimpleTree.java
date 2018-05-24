package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.kaleidoscope.core.auxiliary.simpletree.artefactadapter.XML.XMLArtefactAdapter;
import com.kaleidoscope.core.auxiliary.xmi.artefactadapter.XMIArtefactAdapter;

import Simpletree.Folder;
import Simpletree.TreeElement;
import Simpletree.impl.FolderImpl;

public class XmlToOrFromSimpleTree {

	/**
	 * METHOD FOR CONVERTING XML ARTEFACT TO SIMPLETREE MODEL
	 */
	public void convertXMLToSimpleTree(String workspacePath) {
		// calling XMLAdapter
		Path path = Paths.get(workspacePath);
		XMLArtefactAdapter xmlAdapter = new XMLArtefactAdapter(path);

		// parse XML
		System.out.println("Parsing workspace ...");
		xmlAdapter.parse();

		// save it to src.xmi at path <generatedSimpleTreeModelPath>
		storeSimpleTreeModelToXMI(xmlAdapter);

	}

	/**
	 * Method for saving the model as src.xmi
	 * 
	 * @param xmlAdapter
	 */
	private void storeSimpleTreeModelToXMI(XMLArtefactAdapter xmlAdapter) {
		// store the parsed tree in .xmi file
		Optional<TreeElement> model = xmlAdapter.getModel();
		model.ifPresent(m -> {
			XMIArtefactAdapter<TreeElement> xmiArtefactAdapter = new XMIArtefactAdapter<TreeElement>(
					Paths.get(CONSTANTS.SIMPLE_TREE_XMI_PATH));
			xmiArtefactAdapter.setModel(m);
			xmiArtefactAdapter.unparse();
		});
		System.out.println("Parsing completed ...");
	}

	/**
	 * @param workspacePath 
	 * @param simpleTreeModel
	 */
	public void convertSimpleTreeToXml(TreeElement treeElement, String workspacePath) {
		// calling XMLAdapter
		Path path = Paths.get(workspacePath);
		XMLArtefactAdapter xmlArtefactAdapter = new XMLArtefactAdapter(path);
		xmlArtefactAdapter.setModel(treeElement);
		if(xmlArtefactAdapter.getModel().get() instanceof FolderImpl) {
			String workspaceName = ((Folder)xmlArtefactAdapter.getModel().get()).getName();
			((Folder)xmlArtefactAdapter.getModel().get()).setName(workspacePath+workspaceName);
		}
		xmlArtefactAdapter.unparse();
		System.out.println("WORKSPACE regenrated ...");
	}

}
