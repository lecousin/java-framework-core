package net.lecousin.framework.xml;

import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.util.Pair;
import net.lecousin.framework.xml.XMLStreamEvents.Event;

public interface XMLStreamEventsRecorder {

	public void startRecording(boolean recordCurrentEvent);
	
	public void stopRecording();
	
	public void replay();
	
	public Event getFirstRecordedEvent();
	
	
	public static class Sync extends XMLStreamEventsSync implements XMLStreamEventsRecorder {
		
		public Sync(XMLStreamEventsSync stream) {
			this.stream = stream;
			this.event = stream.event;
		}
		
		protected XMLStreamEventsSync stream;
		protected boolean recording = false;
		protected LinkedList<Event> record = null;
		protected Iterator<Event> replaying = null;
		
		@Override
		public Pair<Integer, Integer> getPosition() {
			return stream.getPosition(); // TODO during replay ?
		}
		
		@Override
		public Event getFirstRecordedEvent() {
			return record != null && !record.isEmpty() ? record.getFirst() : null;
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
		}
		
	}

	public static class Async extends XMLStreamEventsAsync implements XMLStreamEventsRecorder {
		
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
		public SynchronizationPoint<Exception> next() {
			if (replaying != null) {
				if (!replaying.hasNext())
					return new SynchronizationPoint<>(new EOFException());
				event = replaying.next();
				return new SynchronizationPoint<>(true);
			}
			SynchronizationPoint<Exception> next = stream.next();
			next.listenInline(() -> {
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
		}
		
	}
	
}
