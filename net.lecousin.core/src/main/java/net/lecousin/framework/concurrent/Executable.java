package net.lecousin.framework.concurrent;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.Runnables;

/** Executable that returns a result, and throw an Exception or CancelException.
 *
 * @param <T> type of result
 * @param <E> type of exception
 */
public interface Executable<T, E extends Exception> {

	/** Execute. */
	T execute() throws E, CancelException;
	
	/** Executable from a Runnable. */
	class FromRunnable implements Executable<Void, NoException> {
		public FromRunnable(java.lang.Runnable run) {
			this.run = run;
		}
		
		private java.lang.Runnable run;
		
		@Override
		public Void execute() {
			run.run();
			return null;
		}
	}
	
	/** Executable from a Consumer.
	 *
	 * @param <T> type of data consumed
	 */
	class FromConsumer<T> implements Executable<Void, NoException> {
		
		public FromConsumer(Consumer<T> consumer) {
			this.consumer = consumer;
		}
		
		private Consumer<T> consumer;
		private T input;
		
		public void setInput(T input) {
			this.input = input;
		}
		
		@Override
		public Void execute() throws CancelException {
			consumer.accept(input);
			return null;
		}
		
	}
	
	/** Executable from a ConsumerThrows.
	 *
	 * @param <T> type of data consumed
	 * @param <E> type of error
	 */
	class FromConsumerThrows<T, E extends Exception> implements Executable<Void, E> {
		
		public FromConsumerThrows(Runnables.ConsumerThrows<T, E> consumer) {
			this.consumer = consumer;
		}
		
		private Runnables.ConsumerThrows<T, E> consumer;
		private T input;
		
		public void setInput(T input) {
			this.input = input;
		}
		
		@Override
		public Void execute() throws E, CancelException {
			consumer.accept(input);
			return null;
		}
		
	}
	
	/** Executable from a Function.
	 *
	 * @param <IN> type of input for the function
	 * @param <OUT> type of output for the function
	 */
	class FromFunction<IN, OUT> implements Executable<OUT, NoException> {
		
		public FromFunction(Function<IN, OUT> fct) {
			this.fct = fct;
		}
		
		private Function<IN, OUT> fct;
		private IN input;
		
		public void setInput(IN input) {
			this.input = input;
		}
		
		@Override
		public OUT execute() throws CancelException {
			return fct.apply(input);
		}
		
	}
	
	/** Executable from a FunctionThrows.
	 *
	 * @param <IN> type of input for the function
	 * @param <OUT> type of output for the function
	 * @param <E> type of error the function may throw
	 */
	class FromFunctionThrows<IN, OUT, E extends Exception> implements Executable<OUT, E> {
		
		public FromFunctionThrows(Runnables.FunctionThrows<IN, OUT, E> fct) {
			this.fct = fct;
		}
		
		private Runnables.FunctionThrows<IN, OUT, E> fct;
		private IN input;
		
		public void setInput(IN input) {
			this.input = input;
		}
		
		@Override
		public OUT execute() throws E, CancelException {
			return fct.apply(input);
		}
		
	}
	
	/** Executable from SupplierThrows.
	 *
	 * @param <T> type of data supplied
	 */
	class FromSupplier<T> implements Executable<T, NoException> {
		
		public FromSupplier(Supplier<T> supplier) {
			this.supplier = supplier;
		}
		
		private Supplier<T> supplier;
		
		@Override
		public T execute() throws CancelException {
			return supplier.get();
		}
	}
	
	/** Executable from SupplierThrows.
	 *
	 * @param <T> type of data supplied
	 * @param <E> type of error
	 */
	class FromSupplierThrows<T, E extends Exception> implements Executable<T, E> {
		
		public FromSupplierThrows(Runnables.SupplierThrows<T, E> supplier) {
			this.supplier = supplier;
		}
		
		private Runnables.SupplierThrows<T, E> supplier;
		
		@Override
		public T execute() throws E, CancelException {
			return supplier.get();
		}
	}
	
}
