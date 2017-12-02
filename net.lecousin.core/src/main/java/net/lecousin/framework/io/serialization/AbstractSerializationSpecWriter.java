package net.lecousin.framework.io.serialization;

import java.util.List;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.serialization.rules.SerializationRule;

public abstract class AbstractSerializationSpecWriter implements SerializationSpecWriter {

	protected byte priority;
	
	protected abstract ISynchronizationPoint<? extends Exception> initializeSpecWriter(IO.Writable output);
	
	protected abstract ISynchronizationPoint<? extends Exception> finalizeSpecWriter();
	
	protected class SpecTask extends Task.Cpu<Void, NoException> {
		public SpecTask(Runnable r) {
			super("Write Specification", priority);
			this.r = r;
		}
		
		private Runnable r;
		
		@Override
		public Void run() {
			r.run();
			return null;
		}
	}
	
	@Override
	public ISynchronizationPoint<Exception> writeSpecification(Class<?> type, IO.Writable output, List<SerializationRule> rules) {
		priority = output.getPriority();
		ISynchronizationPoint<? extends Exception> init = initializeSpecWriter(output);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		init.listenAsyncSP(new SpecTask(() -> {
			ISynchronizationPoint<? extends Exception> sp = specifyValue(null, type, rules);
			sp.listenInlineSP(() -> {
				finalizeSpecWriter().listenInlineSP(result);
			}, result);
		}), result);
		return result;
	}
	
	protected ISynchronizationPoint<? extends Exception> specifyValue(SerializationContext context, Class<?> type, List<SerializationRule> rules) {
		if (boolean.class.equals(type))
			return specifyBooleanValue(false);
		if (Boolean.class.equals(type))
			return specifyBooleanValue(true);
		
		if (byte.class.equals(type))
			return specifyNumericValue(Byte.class, false, Byte.valueOf(Byte.MIN_VALUE), Byte.valueOf(Byte.MAX_VALUE));
		if (Byte.class.equals(type))
			return specifyNumericValue(Byte.class, true, Byte.valueOf(Byte.MIN_VALUE), Byte.valueOf(Byte.MAX_VALUE));
		if (short.class.equals(type))
			return specifyNumericValue(Short.class, false, Short.valueOf(Short.MIN_VALUE), Short.valueOf(Short.MAX_VALUE));
		if (Short.class.equals(type))
			return specifyNumericValue(Short.class, true, Short.valueOf(Short.MIN_VALUE), Short.valueOf(Short.MAX_VALUE));
		if (int.class.equals(type))
			return specifyNumericValue(Integer.class, false, Integer.valueOf(Integer.MIN_VALUE), Integer.valueOf(Integer.MAX_VALUE));
		if (Integer.class.equals(type))
			return specifyNumericValue(Integer.class, true, Integer.valueOf(Integer.MIN_VALUE), Integer.valueOf(Integer.MAX_VALUE));
		if (long.class.equals(type))
			return specifyNumericValue(Long.class, false, Long.valueOf(Long.MIN_VALUE), Long.valueOf(Long.MAX_VALUE));
		if (Long.class.equals(type))
			return specifyNumericValue(Long.class, true, Long.valueOf(Long.MIN_VALUE), Long.valueOf(Long.MAX_VALUE));

		// TODO
		return new SynchronizationPoint<>(true);
	}
	
	protected abstract ISynchronizationPoint<? extends Exception> specifyBooleanValue(boolean nullable);

	protected abstract ISynchronizationPoint<? extends Exception> specifyNumericValue(Class<?> type, boolean nullable, Number min, Number max);
	
}
