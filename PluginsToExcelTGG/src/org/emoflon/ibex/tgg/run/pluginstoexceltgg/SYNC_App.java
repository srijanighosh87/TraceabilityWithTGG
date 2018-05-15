package org.emoflon.ibex.tgg.run.pluginstoexceltgg;

import java.io.IOException;

import org.apache.log4j.BasicConfigurator;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.emoflon.ibex.tgg.compiler.patterns.PatternSuffixes;
import org.emoflon.ibex.tgg.operational.csp.constraints.factories.UserDefinedRuntimeTGGAttrConstraintFactory;
import org.emoflon.ibex.tgg.operational.defaults.IbexOptions;
import org.emoflon.ibex.tgg.operational.strategies.sync.SYNC;
import org.emoflon.ibex.tgg.runtime.engine.DemoclesTGGEngine;

public class SYNC_App extends SYNC {
	private boolean fwd;

	public SYNC_App(boolean fwd) throws IOException {
		super(createIbexOptions());
		this.fwd = fwd;
		registerBlackInterpreter(new DemoclesTGGEngine());
	}

	public static void main(String[] args) throws IOException {
		BasicConfigurator.configure();
		SYNC_App sync = new SYNC_App(false);
		sync.executeSync(sync);
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

	

	@Override
	public void loadModels() throws IOException {
		if (fwd) {
			s = loadResource(projectPath + "/instances/src.xmi");
			t = createResource(projectPath + "/instances/trg.xmi");
		} else {
			t = loadResource(projectPath + "/instances/trg.xmi");
			s = createResource(projectPath + "/instances/src.xmi");
		}

		c = createResource(projectPath + "/instances/corr.xmi");
		p = createResource(projectPath + "/instances/protocol.xmi");

		EcoreUtil.resolveAll(rs);
	}

	@Override
	public boolean isPatternRelevantForCompiler(String patternName) {
		if (fwd)
			return patternName.endsWith(PatternSuffixes.FWD);
		else
			return patternName.endsWith(PatternSuffixes.BWD);
	}

	protected void registerUserMetamodels() throws IOException {
		_RegistrationHelper.registerMetamodels(rs, this);

		// Register correspondence metamodel last
		loadAndRegisterMetamodel(projectPath + "/model/" + projectPath + ".ecore");
	}

	private static IbexOptions createIbexOptions() {
		IbexOptions options = new IbexOptions();
		options.projectName("PluginsToExcelTGG");
		options.projectPath("PluginsToExcelTGG");
		//change optimizers
		options.minimumNumberOfEdgesToCreateEdgePatterns(5);
		options.setCorrContextNodesAsLocalNodes(true); 
		options.stronglyTypedEdgedPatterns(false);
		
		options.debug(true);
		
		options.userDefinedConstraints(new UserDefinedRuntimeTGGAttrConstraintFactory());
		return options;
	}
}
