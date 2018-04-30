package org.emoflon.ibex.tgg.run.pluginstoexceltgg;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emoflon.ibex.tgg.operational.strategies.OperationalStrategy;

import Simpleexcel.impl.SimpleexcelPackageImpl;
import Simpletree.impl.SimpletreePackageImpl;


public class _RegistrationHelper {

	/** Load and register source and target metamodels */
	public static void registerMetamodels(ResourceSet rs, OperationalStrategy strategy)  throws IOException {
		//throw new NotImplementedException("You need to register your source and target metamodels.");
		
		rs.getPackageRegistry().put("platform:/plugin/com.kaleidoscope.core.aux.simpleexcel/model/Simpleexcel.ecore",
				SimpleexcelPackageImpl.init());
		rs.getPackageRegistry().put(
				"platform:/plugin/com.kaleidoscope.core.aux.simpletree/model/Simpletree.ecore", SimpletreePackageImpl.init());
	}
}
