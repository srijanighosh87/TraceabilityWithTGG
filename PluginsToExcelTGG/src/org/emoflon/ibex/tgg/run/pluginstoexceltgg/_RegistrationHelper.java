package org.emoflon.ibex.tgg.run.pluginstoexceltgg;

import java.io.IOException;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.emoflon.ibex.tgg.operational.csp.constraints.factories.UserDefinedRuntimeTGGAttrConstraintFactory;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
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
	
	/** Create default options **/
	public static IbexOptions createIbexOptions() {
		IbexOptions options = new IbexOptions();
		options.projectName("PluginsToExcelTGG");
		options.projectPath("PluginsToExcelTGG");
		options.debug(false);
		options.userDefinedConstraints(new UserDefinedRuntimeTGGAttrConstraintFactory());
		return options;
	}
}
