package org.emoflon.ibex.tgg.run.pluginstoexceltgg;

import java.io.IOException;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.operational.csp.constraints.factories.UserDefinedRuntimeTGGAttrConstraintFactory;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;
import org.emoflon.ibex.tgg.runtime.engine.DemoclesTGGEngine;

public class SYNC_App extends SYNC {
	private boolean fwd;
	private EObject input;

	public SYNC_App(boolean fwd, EObject input) throws IOException {
		super(createIbexOptions());
		this.fwd = fwd;
		this.input = input;
		registerBlackInterpreter(new DemoclesTGGEngine());
	}

/*	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();
		SYNC_App sync = new SYNC_App(false);
		sync.executeSync(sync);
	}*/

	@Override
	protected Resource loadTGGResource() throws IOException {
		return loadResource(
				"platform:/plugin/" + options.projectName() + "/model/" + options.projectName() + ".tgg.xmi");
	}

	@Override
	protected Resource loadFlattenedTGGResource() throws IOException {
		return loadResource(
				"platform:/plugin/" + options.projectName() + "/model/" + options.projectName() + "_flattened.tgg.xmi");
	}
	
	@Override
	public void loadModels() throws IOException {
		if (fwd) {
			//s = loadResource(options.projectPath() + "/instances/src.xml");
			//t = createResource(options.projectPath() + "/instances/trg.xlsx");
			
			s = createResource("temp/instances/src.xmi");
			s.getContents().add(input);
			t = createResource("temp/instances/trg.xlsx");
			
		} else {
			//t = loadResource(options.projectPath() + "/instances/trg.xlsx");
			// s = createResource(options.projectPath() + "/instances/src.xml");
			
			t = createResource("temp/instances/trg.xlsx");
			t.getContents().add(input);
			s = createResource("temp/instances/src.xmi");
		}

		/*c = createResource(options.projectPath() + "/instances/corr.xmi");
		p = createResource(options.projectPath() + "/instances/protocol.xmi");*/

		c = createResource("temp/instances/corr.xmi");
		p = createResource("temp/instances/protocol.xmi");

		EcoreUtil.resolveAll(rs);
	}
	
	public void executeSync(SYNC_App sync) throws IOException {
		logger.info("Starting SYNC");
		long tic = System.currentTimeMillis();

		try {
			if (this.fwd) {
				System.out.println("Performing forward transformation");
				sync.forward();
			} else {
				System.out.println("Performing backward transformation");
				sync.backward();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		long toc = System.currentTimeMillis();
		logger.info("Completed SYNC in: " + (toc - tic) + " ms");

		sync.saveModels();
		sync.terminate();
	}


	protected void registerUserMetamodels() throws IOException {
		_RegistrationHelper.registerMetamodels(rs, this);

		// Register correspondence metamodel
		Resource res = loadResource("platform:/plugin/" + options.projectName() + "/model/" + options.projectName() + ".ecore");
		EPackage pack = (EPackage) res.getContents().get(0);
		rs.getPackageRegistry().put(pack.getNsURI(), pack);
		rs.getResources().remove(res);
	}


	private static IbexOptions createIbexOptions() {
		IbexOptions options = new IbexOptions();
		options.projectName("PluginsToExcelTGG");
		options.projectPath("PluginsToExcelTGG");

		// change optimizers
		options.minimumNumberOfEdgesToCreateEdgePatterns(5);
		options.setCorrContextNodesAsLocalNodes(true);
		options.stronglyTypedEdgedPatterns(false);

		options.debug(true);

		options.userDefinedConstraints(new UserDefinedRuntimeTGGAttrConstraintFactory());
		return options;
	}
	
	@Override
	public boolean isPatternRelevantForCompiler(String patternName) {
		if (fwd)
			return patternName.endsWith(PatternSuffixes.FWD);
		else
			return patternName.endsWith(PatternSuffixes.BWD);
	}
}
