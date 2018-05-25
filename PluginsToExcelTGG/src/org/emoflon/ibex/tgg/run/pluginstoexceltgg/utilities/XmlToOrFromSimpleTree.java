package org.emoflon.ibex.tgg.run.pluginstoexceltgg.utilities;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kaleidoscope.core.auxiliary.simpletree.artefactadapter.XML.XMLArtefactAdapter;
import com.kaleidoscope.core.auxiliary.xmi.artefactadapter.XMIArtefactAdapter;

import Simpleexcel.ExcelElement;
import Simpleexcel.File;
import Simpletree.Folder;
import Simpletree.TreeElement;
import Simpletree.impl.FolderImpl;

public class XmlToOrFromSimpleTree {

	/**
	 * METHOD FOR CONVERTING XML ARTEFACT TO SIMPLETREE MODEL
	 * @return 
	 */
	public Optional<TreeElement> convertXMLToSimpleTree(String workspacePath) {
		// calling XMLAdapter
		Path path = Paths.get(workspacePath);
		XMLArtefactAdapter xmlAdapter = new XMLArtefactAdapter(path);

		// parse XML
		System.out.println("Parsing workspace ...");
		xmlAdapter.parse();

		// save it to src.xmi at path <generatedSimpleTreeModelPath>
		//storeSimpleTreeModelToXMI(xmlAdapter);

		return xmlAdapter.getModel();
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
		if (xmlArtefactAdapter.getModel().get() instanceof FolderImpl) {
			String workspaceName = ((Folder) xmlArtefactAdapter.getModel().get()).getName();
			if (workspaceName != null && workspaceName.equalsIgnoreCase("")
					&& !checkLegalDriveOrFolderName(workspaceName))
				((Folder) xmlArtefactAdapter.getModel().get()).setName(workspacePath + workspaceName);
			else
				((Folder) xmlArtefactAdapter.getModel().get()).setName(workspacePath);

		}
		xmlArtefactAdapter.unparse();
		System.out.println("WORKSPACE regenrated ...");
	}

	/**
	 * check for forbidden characters [\/:*?"<>|] & folder name can not be "."
	 * 
	 * @param folderName
	 * @return
	 */
	private boolean checkLegalDriveOrFolderName(String folderName) {
		// : is ignored in this folder name, because in windows C: gives drive name and
		// this regex is used for legal driveName or folder name
		final String regex = "^[^\\/\\\\*?\"<>|]{1,}$";
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(folderName);
		while (matcher.find() && !folderName.equalsIgnoreCase(".") && !folderName.equalsIgnoreCase(":")) {
			return true;
		}
		return false;
	}

}
