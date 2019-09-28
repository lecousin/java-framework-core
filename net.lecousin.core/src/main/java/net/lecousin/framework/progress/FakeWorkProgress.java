package net.lecousin.framework.progress;

import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.CancelException;
import net.lecousin.framework.concurrent.async.IAsync;

/** Implementation of WorkProgress without really increasing any progression. */
public class FakeWorkProgress implements WorkProgress {

	private Async<Exception> synch = new Async<>(true);
	
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
	public IAsync<Exception> getSynch() {
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
