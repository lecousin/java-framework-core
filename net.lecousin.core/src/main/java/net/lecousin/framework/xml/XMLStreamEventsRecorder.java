package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.xml.XMLStreamEvents.Event;

/**
 * An XMLStreamEventsRecorder allows to wrap an XMLStreamEvents, record each events, and be able to replay them.
 * Because an XMLStreamEvents goes always forward, it may be sometimes useful to be able to replay those events,
 * either on the full XML document, or on a sub-part.<br/>
 * Both synchronous and asynchronous implementations are available as inner classes of this interface.
 */
public interface XMLStreamEventsRecorder {

	/** Start to record events.
	 * @param recordCurrentEvent true to save the current event, false to start saving events for the next one.
	 */
	void startRecording(boolean recordCurrentEvent);
	
	/** Stop recording events. */
	void stopRecording();
	
	/** Start to replay.
	 * Once this method is called, when reaching the last recorded event an EOFException will be raised.
	 * This method may be called multiple times to replay several times the recorded events. It will restart
	 * from the first recorded event at each call.
	 */
	void replay();
	
	/** Shortcut to know what is the first recorded event. */
	Event getFirstRecordedEvent();
	
	/** Synchronous implementation of the recorder. */
	public static class Sync extends XMLStreamEventsSync implements XMLStreamEventsRecorder {
		
		/** Constructor. */
		public Sync(XMLStreamEventsSync stream) {
			this.stream = stream;
			this.event = stream.event;
		}
		
		protected XMLStreamEventsSync stream;
		protected boolean recording = false;
		protected LinkedList<Event> record = null;
		protected Iterator<Event> replaying = null;
		
		@Override
		public Event getFirstRecordedEvent() {
			return record != null && !record.isEmpty() ? record.getFirst() : null;
		}
		
		@Override
		public void start() throws XMLException, IOException {
			stream.start();
		}
		
		@Override
		public void next() throws XMLException, IOException {
			if (replaying != null) {
				if (!replaying.hasNext())
					throw new EOFException();
				event = replaying.next();
				return;
			}
			stream.next();
			event = stream.event;
			if (recording)
				record.add(stream.event.copy());
		}
		
		@Override
		public void startRecording(boolean recordCurrentEvent) {
			recording = true;
			record = new LinkedList<>();
			if (recordCurrentEvent)
				record.add(stream.event.copy());
		}
		
		@Override
		public void stopRecording() {
			recording = false;
		}
		
		@Override
		public void replay() {
			replaying = record.iterator();
			if (!replaying.hasNext())
				event = null;
			else
				event = replaying.next();
		}
		
	}

	/** Asynchronous implementation of the recorder. */
	public static class Async extends XMLStreamEventsAsync implements XMLStreamEventsRecorder {
		
		/** Constructor. */
		public Async(XMLStreamEventsAsync stream) {
			this.stream = stream;
			this.event = stream.event;
		}
		
		protected XMLStreamEventsAsync stream;
		protected boolean recording = false;
		protected LinkedList<Event> record = null;
		protected Iterator<Event> replaying = null;
		
		@Override
		public byte getPriority() {
			return stream.getPriority();
		}
		
		@Override
		public Event getFirstRecordedEvent() {
			return record != null && !record.isEmpty() ? record.getFirst() : null;
		}
		
		@Override
		public IAsync<Exception> start() {
			return stream.start();
		}

		@Override
		public net.lecousin.framework.concurrent.async.Async<Exception> next() {
			if (replaying != null) {
				if (!replaying.hasNext())
					return new net.lecousin.framework.concurrent.async.Async<>(new EOFException());
				event = replaying.next();
				return new net.lecousin.framework.concurrent.async.Async<>(true);
			}
			net.lecousin.framework.concurrent.async.Async<Exception> next = stream.next();
			next.onDone(() -> {
				if (next.isSuccessful()) {
					event = stream.event;
					if (recording)
						record.add(stream.event.copy());
				}
			});
			return next;
		}
		
		@Override
		public void startRecording(boolean recordCurrentEvent) {
			recording = true;
			record = new LinkedList<>();
			if (recordCurrentEvent)
				record.add(stream.event.copy());
		}
		
		@Override
		public void stopRecording() {
			recording = false;
		}
		
		@Override
		public void replay() {
			replaying = record.iterator();
			if (!replaying.hasNext())
				event = null;
			else
				event = replaying.next();
		}
		
	}
	
}
