package net.lecousin.framework.concurrent.util.production.simple;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.AsyncWork.AsyncWorkListener;

/**
 * Implement a consumer for a specific type, and transform it before to pass it to another consumer.
 * @param <Input> type of object consumed
 * @param <Output> output of the transformation, which is the input of the embedded consumer 
 */
public abstract class ProductTransformation<Input,Output> implements Consumer<Input> {

	/** Constructor. */
	public ProductTransformation(Consumer<Output> consumer) {
		this.consumer = consumer;
	}
	
	private Consumer<Output> consumer;
	private AsyncWork<?,? extends Exception> consuming = null;
	private Output waitingData = null;
	private CancelException cancelled = null;
	private Exception error = null;
	private AsyncWork<?,? extends Exception> endReached = null;
	
	@Override
	public AsyncWork<?, ? extends Exception> consume(Input product) {
		AsyncWork<Void,Exception> sp = new AsyncWork<>();
		if (error != null) {
			sp.unblockError(error);
			return sp;
		}
		if (cancelled != null) {
			sp.unblockCancel(cancelled);
			return sp;
		}
		AsyncWork<Output,Exception> processing = process(product);
		processing.listenInline(new AsyncWorkListener<Output, Exception>() {
			@Override
			public void ready(Output result) {
				synchronized (consumer) {
					if (error != null) {
						sp.unblockError(error);
						return;
					}
					if (cancelled != null) {
						sp.unblockCancel(cancelled);
						return;
					}
					if (consuming == null)
						consumeTransformed(result, sp);
					else
						waitingData = result;
				}
			}
			
			@Override
			public void cancelled(CancelException event) {
				cancel(event);
				sp.unblockCancel(event);
			}
			
			@Override
			public void error(Exception error) {
				ProductTransformation.this.error(error);
				sp.unblockError(error);
			}
		});
		return sp;
	}
	
	protected abstract AsyncWork<Output,Exception> process(Input input);
	
	private void consumeTransformed(Output data, AsyncWork<Void,Exception> sp) {
		consuming = consumer.consume(data);
		sp.unblockSuccess(null);
		consuming.listenInline(() -> {
			synchronized (consumer) {
				if (error != null) {
					sp.unblockError(error);
					return;
				}
				if (cancelled != null) {
					sp.unblockCancel(cancelled);
					return;
				}
				if (!consuming.isSuccessful()) {
					if (consuming.isCancelled())
						cancel(consuming.getCancelEvent());
					else
						error(consuming.getError());
					consuming = null;
					return;
				}
				consuming = null;
				if (waitingData != null) {
					consumeTransformed(waitingData, sp);
					waitingData = null;
				} else if (endReached != null) {
					consumer.endOfProduction().listenInline(() -> endReached.unblockSuccess(null));
				}
			}
		});
	}

	@Override
	public void cancel(CancelException event) {
		cancelled = event;
	}
	
	@Override
	public void error(Exception error) {
		this.error = error;
	}
	
	@Override
	public AsyncWork<?, ? extends Exception> endOfProduction() {
		synchronized (consumer) {
			if (consuming == null)
				return consumer.endOfProduction();
			endReached = new AsyncWork<>();
			return endReached;
		}
	}
}
