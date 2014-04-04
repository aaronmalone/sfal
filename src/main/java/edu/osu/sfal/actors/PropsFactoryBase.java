package edu.osu.sfal.actors;

import akka.actor.Props;
import org.apache.commons.lang3.Validate;

public abstract class PropsFactoryBase<T> implements PropsFactory<T> {

	private final Class<T> cls;

	protected PropsFactoryBase(Class<T> cls) {
		Validate.notNull(cls, "Should not construct with a null class.");
		this.cls = cls;
	}

	@Override
	public final Props createProps(Class<T> cls, Object... argsPassedIn) {
		Validate.isTrue(this.cls.equals(cls), "Class passed to createProps did not match expected class: " + this.cls);
		Object[] argsToUse = getArgsToUse(argsPassedIn);
		return Props.create(cls, argsToUse);
	}

	protected abstract Object[] getArgsToUse(Object[] argsPassedIn);

	protected final <I> I validateClassOfInputArgument(int index, Object[] arguments, Class<I> expectedClass) {
		Validate.notNull(expectedClass, "Class passed to validateClassOfInputArgument should not be null");
		Validate.isTrue(index >= 0, "Passed negative index to validateClassOfInputArgument");
		Validate.isTrue(index < arguments.length, "Invalid index for arguments array");
		Object argument = arguments[index];
		if(expectedClass.isAssignableFrom(argument.getClass())) {
			return expectedClass.cast(argument);
		} else {
			throw new IllegalArgumentException("Argument " + index + " was not of expected type " + expectedClass);
		}
	}
}
