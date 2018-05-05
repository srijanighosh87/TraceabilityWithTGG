package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import com.kaleidoscope.core.auxiliary.simpletree.artefactadapter.XML.XMLArtefactAdapter;
import com.kaleidoscope.core.auxiliary.xmi.artefactadapter.XMIArtefactAdapter;

import Simpletree.TreeElement;

public class XmlToOrFromSimpleTree {
	private static String INPUT_WORKSPACE_PATH = "C:\\Users\\Srijani\\Desktop\\Workspace";
	private static String SIMPLE_TREE_XMI_PATH = "./instances/src.xmi";
	
	/**
	 * METHOD FOR CONVERTING XML ARTEFACT TO SIMPLETREE MODEL
	 */
	public void convertXMLToSimpleTree() {
		// calling XMLAdapter
		Path path = Paths.get(INPUT_WORKSPACE_PATH);
		XMLArtefactAdapter xmlAdapter = new XMLArtefactAdapter(path);

		// parse XML
		System.out.println("Parsing XML file ...");
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
					Paths.get(SIMPLE_TREE_XMI_PATH));
			xmiArtefactAdapter.setModel(m);
			xmiArtefactAdapter.unparse();
		});
		System.out.println("Parsing completed ...");
	}

}
