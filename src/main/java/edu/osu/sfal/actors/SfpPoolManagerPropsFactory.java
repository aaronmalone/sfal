package edu.osu.sfal.actors;

import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;

public class SfpPoolManagerPropsFactory extends PropsFactoryBase<SfpPoolManager> {

	private final PropsFactory<SfpActor> sfpActorPropsFactory;

	public SfpPoolManagerPropsFactory(PropsFactory<SfpActor> sfpActorPropsFactory) {
		super(SfpPoolManager.class);
		this.sfpActorPropsFactory = sfpActorPropsFactory;
	}

	@Override
	protected Object[] getArgsToUse(Object[] argsPassedIn) {
		Validate.isTrue(argsPassedIn.length ==1);
		SimulationFunctionName funName = validateClassOfInputArgument(0, argsPassedIn, SimulationFunctionName.class);
		return new Object[]{funName, this.sfpActorPropsFactory};
	}
}
