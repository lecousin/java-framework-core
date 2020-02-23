package net.lecousin.framework.progress;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.event.AsyncEvent;
import net.lecousin.framework.event.SimpleListenableContainer;

/** Default implementation of a WorkProgress. */
public class WorkProgressImpl extends SimpleListenableContainer<AsyncEvent> implements WorkProgress {

	/** Constructor. */
	public WorkProgressImpl(long amount, String text, String subText) {
		this.amount = amount;
		this.text = text;
		this.subText = subText;
	}

	/** Constructor. */
	public WorkProgressImpl(long amount, String text) {
		this(amount, text, "");
	}

	/** Constructor. */
	public WorkProgressImpl(long amount) {
		this(amount, "", "");
	}
	
	protected long amount;
	protected long position = 0;
	protected String text;
	protected String subText;
	protected Async<Exception> synch = new Async<>();
	protected boolean eventsInterrupted = false;
	
	@Override
	protected AsyncEvent createEvent() {
		return new AsyncEvent();
	}
	
	@Override
	public void setAmount(long work) {
		if (work == amount) return;
		this.amount = work;
		if (position > work) position = work;
		changed();
	}
	
	@Override
	public long getAmount() {
		return amount;
	}
	
	@Override
	public void setPosition(long position) {
		if (position > amount) position = amount;
		if (position < 0) position = 0;
		if (position == this.position) return;
		this.position = position;
		changed();
	}
	
	@Override
	public long getPosition() {
		return position;
	}
	
	@Override
	public void progress(long amountDone) {
		setPosition(position + amountDone);
	}
	
	@Override
	public void done() {
		position = amount;
		synch.unblock();
		changed();
	}
	
	@Override
	public void error(Exception error) {
		synch.error(error);
		changed();
	}
	
	@Override
	public void cancel(CancelException reason) {
		synch.cancel(reason);
		changed();
	}
	
	@Override
	public IAsync<Exception> getSynch() {
		return synch;
	}
	
	@Override
	public long getRemainingWork() {
		return amount - position;
	}
	
	@Override
	public String getText() {
		return text;
	}
	
	@Override
	public void setText(String text) {
		this.text = text;
		changed();
	}
	
	@Override
	public String getSubText() {
		return subText;
	}
	
	@Override
	public void setSubText(String text) {
		this.subText = text;
		changed();
	}
	
	@Override
	public void interruptEvents() {
		eventsInterrupted = true;
	}
	
	@Override
	public void resumeEvents(boolean trigger) {
		eventsInterrupted = false;
		if (trigger)
			changed();
	}
	
	protected void changed() {
		if (eventsInterrupted) return;
		synchronized (this) {
			if (event != null) event.fire();
		}
	}
}
