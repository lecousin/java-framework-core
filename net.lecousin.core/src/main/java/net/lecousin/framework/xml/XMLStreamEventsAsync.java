package net.lecousin.framework.xml;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.collections.CollectionsUtil;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;

/** Base class for asynchronous implementations of XMLStreamEvents. */
public abstract class XMLStreamEventsAsync extends XMLStreamEvents {

	/** Start reading the XML to provide the first event.
	 * If the first tag is a processing instruction XML it reads it and goes to the next event.
	 */
	public abstract IAsync<Exception> start();
	
	/** Return the priority used for tasks. */
	public abstract byte getPriority();
	
	/** Move forward to the next event.
	 * If the next event can be read synchronously, the result is unblocked, else the caller has to wait for it.
	 */
	public abstract Async<Exception> next();
	
	/** Shortcut to move forward to the next START_ELEMENT. */
	public IAsync<Exception> nextStartElement() {
		IAsync<Exception> next = next();
		if (next.isDone()) {
			if (next.hasError()) return next;
			if (Type.START_ELEMENT.equals(event.type)) return next;
			return nextStartElement();
		}
		Async<Exception> sp = new Async<>();
		next.onDone(
			() -> {
				if (Type.START_ELEMENT.equals(event.type)) {
					sp.unblock();
					return;
				}
				new Next(sp) {
					@Override
					protected void onNext() {
						if (Type.START_ELEMENT.equals(event.type)) sp.unblock();
						else nextStartElement().onDone(sp);
					}
				}.start();
			},
			sp
		);
		return sp;
	}
	
	/** Go to the next inner element. The result is false if the parent element has been closed. */
	public AsyncSupplier<Boolean, Exception> nextInnerElement(ElementContext parent) {
		if (event.context.isEmpty() ||
			(Type.START_ELEMENT.equals(event.type) && event.context.getFirst() == parent && event.isClosed) ||
			(Type.END_ELEMENT.equals(event.type) && event.context.getFirst() == parent))
			return new AsyncSupplier<>(Boolean.FALSE, null);
		if (!CollectionsUtil.containsInstance(event.context, parent))
			return new AsyncSupplier<>(null, new Exception("Invalid context: parent element "
				+ parent.localName + " is not in the current context"));
		IAsync<Exception> next = next();
		do {
			if (next.isDone()) {
				if (next.hasError()) return new AsyncSupplier<>(null, next.getError());
				if (Type.END_ELEMENT.equals(event.type)) {
					if (event.context.getFirst() == parent)
						return new AsyncSupplier<>(Boolean.FALSE, null);
				} else if (Type.START_ELEMENT.equals(event.type) && event.context.size() > 1 && event.context.get(1) == parent) {
					return new AsyncSupplier<>(Boolean.TRUE, null);
				}
				next = next();
				continue;
			}
			break;
		} while (true);
		AsyncSupplier<Boolean, Exception> result = new AsyncSupplier<>();
		IAsync<Exception> n = next;
		next.onDone(
			() -> {
				if (n.hasError()) result.error(n.getError());
				else if (Type.END_ELEMENT.equals(event.type) && event.context.getFirst() == parent)
					result.unblockSuccess(Boolean.FALSE);
				else if (Type.START_ELEMENT.equals(event.type) && event.context.size() > 1 && event.context.get(1) == parent)
					result.unblockSuccess(Boolean.TRUE);
				else new ParsingTask(() -> nextInnerElement(parent).forward(result)).start();
			}, result
		);
		return result;
	}
	
	/** Go to the next inner element having the given name. The result is false if the parent element has been closed. */
	public AsyncSupplier<Boolean, Exception> nextInnerElement(ElementContext parent, String childName) {
		AsyncSupplier<Boolean, Exception> next = nextInnerElement(parent);
		do {
			if (next.isDone()) {
				if (next.hasError()) return next;
				if (!next.getResult().booleanValue()) return next;
				if (event.text.equals(childName)) return next;
				next = nextInnerElement(parent);
				continue;
			}
			break;
		} while (true);
		AsyncSupplier<Boolean, Exception> result = new AsyncSupplier<>();
		AsyncSupplier<Boolean, Exception> n = next;
		next.onDone(
			() -> {
				if (n.hasError()) result.error(n.getError());
				else if (!n.getResult().booleanValue()) result.unblockSuccess(Boolean.FALSE);
				else if (event.text.equals(childName)) result.unblockSuccess(Boolean.TRUE);
				else new ParsingTask(() -> nextInnerElement(parent, childName).forward(result)).start();
			}, result
		);
		return result;
	}
	
	protected boolean check(IAsync<Exception> op, IAsync<Exception> result) {
		if (op.hasError()) {
			result.error(op.getError());
			return false;
		}
		return true;
	}
	
	/** Read inner text and close element. */
	public AsyncSupplier<UnprotectedStringBuffer, Exception> readInnerText() {
		if (!Type.START_ELEMENT.equals(event.type))
			return new AsyncSupplier<>(null, new Exception("Invalid call of readInnerText: it must be called on a start element"));
		if (event.isClosed)
			return new AsyncSupplier<>(new UnprotectedStringBuffer(), null);
		UnprotectedStringBuffer innerText = new UnprotectedStringBuffer();
		AsyncSupplier<UnprotectedStringBuffer, Exception> result = new AsyncSupplier<>();
		readInnerText(innerText, result);
		return result;
	}
	
	private void readInnerText(UnprotectedStringBuffer innerText, AsyncSupplier<UnprotectedStringBuffer, Exception> result) {
		IAsync<Exception> next = next();
		do {
			if (next.isDone()) {
				if (!check(next, result)) return;
				if (Type.COMMENT.equals(event.type)) {
					next = next();
				} else if (Type.TEXT.equals(event.type)) {
					innerText.append(event.text);
					next = next();
				} else if (Type.START_ELEMENT.equals(event.type)) {
					if (event.isClosed) {
						next = next();
					} else {
						closeElement().thenStart(new ParsingTask(() -> readInnerText(innerText, result)), result);
						return;
					}
				} else if (Type.END_ELEMENT.equals(event.type)) {
					result.unblockSuccess(innerText);
					return;
				} else {
					next = next();
				}
				continue;
			}
			break;
		} while (true);
		next.onDone(() -> {
			if (Type.START_ELEMENT.equals(event.type)) {
				if (event.isClosed) {
					new ParsingTask(() -> readInnerText(innerText, result)).start();
					return;
				}
				closeElement().thenStart(new ParsingTask(() -> readInnerText(innerText, result)), result);
				return;
			}
			if (Type.COMMENT.equals(event.type)) {
				new ParsingTask(() -> readInnerText(innerText, result)).start();
			} else if (Type.TEXT.equals(event.type)) {
				innerText.append(event.text);
				new ParsingTask(() -> readInnerText(innerText, result)).start();
			} else if (Type.END_ELEMENT.equals(event.type)) {
				result.unblockSuccess(innerText);
			} else {
				new ParsingTask(() -> readInnerText(innerText, result)).start();
			}
		}, result);
	}
	
	/** Go the the END_ELEMENT event corresponding to the current START_ELEMENT (must be called with a current event to START_ELEMENT). */
	public IAsync<Exception> closeElement() {
		if (!Type.START_ELEMENT.equals(event.type))
			return new Async<>(new Exception("Invalid call of closeElement: it must be called on a start element"));
		if (event.isClosed)
			return new Async<>(true);
		ElementContext ctx = event.context.getFirst();
		return closeElement(ctx);
	}
	
	/** Move forward until the closing tag of the given element is found. */
	public IAsync<Exception> closeElement(ElementContext ctx) {
		IAsync<Exception> next = next();
		do {
			if (!next.isDone()) break;
			if (next.hasError()) return next;
			if (Type.END_ELEMENT.equals(event.type) && event.context.getFirst() == ctx)
				return next;
			next = next();
		} while (true);
		Async<Exception> result = new Async<>();
		IAsync<Exception> n = next;
		next.onDone(() -> {
			if (!check(n, result)) return;
			if (Type.END_ELEMENT.equals(event.type) && event.context.getFirst() == ctx) {
				result.unblock();
				return;
			}
			new ParsingTask(() -> closeElement(ctx).onDone(result)).start();
		}, result);
		return result;
	}

	/** Move forward until an element with the given name is found (whatever its depth).
	 */
	public IAsync<Exception> searchElement(String elementName) {
		IAsync<Exception> next = next();
		do {
			if (!next.isDone()) break;
			if (next.hasError()) return next;
			if (Type.START_ELEMENT.equals(event.type) && event.text.equals(elementName)) return next;
			next = next();
		} while (true);
		Async<Exception> result = new Async<>();
		IAsync<Exception> n = next;
		next.onDone(() -> {
			if (n.hasError()) result.error(n.getError());
			else if (Type.START_ELEMENT.equals(event.type) && event.text.equals(elementName)) result.unblock();
			else new ParsingTask(() -> searchElement(elementName).onDone(result)).start();
		}, result);
		return result;
	}
	
	/** Go successively into the given elements. */
	public AsyncSupplier<Boolean, Exception> goInto(ElementContext rootContext, String... innerElements) {
		return goInto(rootContext, 0, innerElements);
	}
	
	private AsyncSupplier<Boolean, Exception> goInto(ElementContext parent, int i, String... innerElements) {
		AsyncSupplier<Boolean, Exception> next = nextInnerElement(parent, innerElements[i]);
		do {
			if (!next.isDone()) break;
			if (next.hasError()) return next;
			if (!next.getResult().booleanValue()) return next;
			i++;
			if (i == innerElements.length) return next;
			parent = event.context.getFirst();
			next = nextInnerElement(parent, innerElements[i]);
		} while (true);
		AsyncSupplier<Boolean, Exception> result = new AsyncSupplier<>();
		AsyncSupplier<Boolean, Exception> n = next;
		int ii = i;
		next.onDone(() -> {
			if (n.hasError()) result.error(n.getError());
			else if (!n.getResult().booleanValue()) result.unblockSuccess(Boolean.FALSE);
			else if (ii == innerElements.length - 1) result.unblockSuccess(Boolean.TRUE);
			else new ParsingTask(() -> goInto(event.context.getFirst(), ii + 1, innerElements).forward(result)).start();
		}, result);
		return result;
	}
	
	/** Read all inner elements with their text, and return a mapping with the element's name as key and inner text as value. */
	public AsyncSupplier<Map<String,String>, Exception> readInnerElementsText() {
		AsyncSupplier<Map<String,String>, Exception> result = new AsyncSupplier<>();
		Map<String, String> texts = new HashMap<>();
		readInnerElementsText(event.context.getFirst(), texts, result);
		return result;
	}
	
	private void readInnerElementsText(ElementContext parent, Map<String, String> texts, AsyncSupplier<Map<String,String>, Exception> result) {
		AsyncSupplier<Boolean, Exception> next = nextInnerElement(parent);
		do {
			if (!next.isDone()) break;
			if (!check(next, result)) return;
			if (!next.getResult().booleanValue()) {
				result.unblockSuccess(texts);
				return;
			}
			String name = event.text.asString();
			AsyncSupplier<UnprotectedStringBuffer, Exception> read = readInnerText();
			if (read.isDone()) {
				if (!check(read, result)) return;
				texts.put(name, read.getResult().asString());
				next = nextInnerElement(parent);
				continue;
			}
			read.onDone(value -> {
				texts.put(name,  value.asString());
				new ParsingTask(() -> readInnerElementsText(parent, texts, result)).start();
			}, result);
			return;
		} while (true);
		next.onDone(res -> {
			if (!res.booleanValue()) {
				result.unblockSuccess(texts);
				return;
			}
			String name = event.text.asString();
			new ParsingTask(() -> {
				AsyncSupplier<UnprotectedStringBuffer, Exception> read = readInnerText();
				if (read.isDone()) {
					if (!check(read, result)) return;
					texts.put(name, read.getResult().asString());
					readInnerElementsText(parent, texts, result);
					return;
				}
				read.onDone(value -> {
					texts.put(name,  value.asString());
					new ParsingTask(() -> readInnerElementsText(parent, texts, result)).start();
				}, result);
			}).start();
		}, result);
	}

	/** Shortcut class to create a task. */
	protected class ParsingTask extends Task.Cpu<Void, NoException> {
		public ParsingTask(Runnable r) {
			super("Parse XML", XMLStreamEventsAsync.this.getPriority());
			this.r = r;
		}
		
		private Runnable r;
		
		@Override
		public Void run() {
			r.run();
			return null;
		}
	}
	
	/** Shortcut to create a task going to the next event, and call onNext if an event is successfully found. */
	protected class Next extends ParsingTask {
		public Next(Async<Exception> sp) {
			super(null);
			this.sp = sp;
		}
		
		protected Async<Exception> sp;
		
		@Override
		public Void run() {
			IAsync<Exception> next = next();
			if (next.isDone()) {
				if (next.hasError()) sp.error(next.getError());
				else onNext();
				return null;
			}
			next.thenStart(new Task.Cpu<Void, NoException>("Parse XML", XMLStreamEventsAsync.this.getPriority()) {
				@Override
				public Void run() {
					onNext();
					return null;
				}
			}, sp);
			return null;
		}
		
		protected void onNext() {
			sp.unblock();
		}
	}
	
}
