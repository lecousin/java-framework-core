package net.lecousin.framework.concurrent.util.production.simple;

import net.lecousin.framework.collections.TurnArray;
import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.AsyncWork;

/**
 * Production model: a producer produces objects that are consumed by a consumer.<br/>
 * This allows asynchronous production and consumption.<br/>
 * An example is a server task producing an output, and a consumer sending it over the network.
 * @param <T> type of object produced and consumed
 */
public class Production<T> {

	/** Constructor. */
	public Production(Producer<T> producer, int maxPending, Consumer<T> consumer) {
		production = new TurnArray<>(maxPending);
		this.producer = producer;
		this.consumer = consumer;
	}
	
	private TurnArray<T> production;
	private boolean endReached = false;
	private Producer<T> producer;
	private Consumer<T> consumer;
	private AsyncWork<?,? extends Exception> consuming = null;
	private AsyncWork<T,? extends Exception> producing = null;
	private AsyncWork<Void,Exception> spEnd = new AsyncWork<>();
	
	/** Start the production. */
	public void start() {
		produce();
	}

	/**
	 * To be called by the producer if and only if it detects the end of the production, but it has a last product to provide.
	 * In case the producer detects the end of the production, and it has not a last product, it must provide null as the new
	 * product.
	 */
	public void endOfProduction() {
		synchronized (production) {
			endReached = true;
		}
	}
	
	public AsyncWork<Void,Exception> getSyncOnFinished() {
		return spEnd;
	}
	
	private void produce() {
		if (endReached) return;
		producing = producer.produce(this);
		producing.listenInline(new Runnable() {
			@Override
			public void run() {
				if (!producing.isSuccessful()) {
					if (producing.isCancelled()) {
						consumer.cancel(producing.getCancelEvent());
						spEnd.unblockCancel(producing.getCancelEvent());
					} else {
						spEnd.unblockError(producing.getError());
						consumer.error(producing.getError());
					}
					return;
				}
				T result = producing.getResult();
				boolean canProduceAgain = false;
				synchronized (production) {
					producing = null;
					if (result == null) {
						endReached = true;
						if (production.isEmpty()) {
							if (consuming == null)
								end();
						}
					} else {
						if (consuming == null)
							consume(result);
						else
							production.addLast(result);
						canProduceAgain = !production.isFull() && producing == null && !endReached;
					}
					if (canProduceAgain) produce();
				}
			}
		});
	}
	
	private void consume(T product) {
		consuming = consumer.consume(product);
		consuming.listenInline(new Runnable() {
			@Override
			public void run() {
				if (!consuming.isSuccessful()) {
					if (consuming.isCancelled()) {
						producer.cancel(consuming.getCancelEvent());
						spEnd.unblockCancel(consuming.getCancelEvent());
					} else {
						producer.cancel(new CancelException("Error", consuming.getError()));
						spEnd.unblockError(consuming.getError());
					}
					return;
				}
				synchronized (production) {
					consuming = null;
					if (!production.isEmpty()) {
						consume(production.removeFirst());
						if (producing == null && !endReached)
							produce();
					} else if (producing == null) {
						if (endReached)
							end();
						else
							produce();
					}
				}
			}
		});
	}
	
	private boolean ended = false;
	
	private void end() {
		if (ended) return;
		ended = true;
		consumer.endOfProduction().listenInline(new Runnable() {
			@Override
			public void run() {
				spEnd.unblockSuccess(null);
			}
		});
	}
	
}
