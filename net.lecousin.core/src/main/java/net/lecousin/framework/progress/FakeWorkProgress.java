package net.lecousin.framework.progress;

import net.lecousin.framework.concurrent.CancelException;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;

/** Implementation of WorkProgress without really increasing any progression. */
public class FakeWorkProgress implements WorkProgress {

	private SynchronizationPoint<Exception> synch = new SynchronizationPoint<>(true);
	
	@Override
	public long getPosition() {
		return 0;
	}
	
	@Override
	public long getAmount() {
		return 0;
	}
	
	@Override
	public long getRemainingWork() {
		return 0;
	}
	
	@Override
	public String getText() {
		return "";
	}
	
	@Override
	public String getSubText() {
		return "";
	}
	
	@Override
	public void progress(long amountDone) {
		// fake
	}
	
	@Override
	public void done() {
		// fake
	}
	
	@Override
	public void error(Exception error) {
		// fake
	}
	
	@Override
	public void cancel(CancelException reason) {
		// fake
	}
	
	@Override
	public ISynchronizationPoint<Exception> getSynch() {
		return synch;
	}
	
	@Override
	public void setAmount(long work) {
		// fake
	}
	
	@Override
	public void setPosition(long position) {
		// fake
	}
	
	@Override
	public void setSubText(String text) {
		// fake
	}
	
	@Override
	public void setText(String text) {
		// fake
	}
	
	@Override
	public void listen(Runnable onchange) {
		// fake
	}
	
	@Override
	public void unlisten(Runnable onchange) {
		// fake
	}
	
	@Override
	public void interruptEvents() {
		// fake
	}
	
	@Override
	public void resumeEvents(boolean trigger) {
		// fake
	}
}
