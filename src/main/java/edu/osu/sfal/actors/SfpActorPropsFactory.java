package edu.osu.sfal.actors;

import edu.osu.lapis.LapisApi;
import edu.osu.sfal.util.SfpName;
import edu.osu.sfal.util.SimulationFunctionName;
import org.apache.commons.lang3.Validate;

public class SfpActorPropsFactory extends PropsFactoryBase<SfpActor> {

	private final LapisApi lapisApi;

	public SfpActorPropsFactory(LapisApi lapisApi) {
		super(SfpActor.class);
		this.lapisApi = lapisApi;
	}

	@Override
	protected Object[] getArgsToUse(Object[] argsPassedIn) {
		Validate.isTrue(argsPassedIn.length == 2);
		SimulationFunctionName funName = validateClassOfInputArgument(0, argsPassedIn, SimulationFunctionName.class);
		SfpName sfp = validateClassOfInputArgument(1, argsPassedIn, SfpName.class);
		return new Object[] {funName, sfp, this.lapisApi};
	}
}
