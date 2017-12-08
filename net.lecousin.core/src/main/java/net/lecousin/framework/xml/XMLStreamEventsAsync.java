package net.lecousin.framework.xml;

import java.util.HashMap;
import java.util.Map;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.ISynchronizationPoint;
import net.lecousin.framework.concurrent.synch.SynchronizationPoint;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.util.UnprotectedStringBuffer;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;

public abstract class XMLStreamEventsAsync extends XMLStreamEvents {

	public abstract byte getPriority();
	
	/** Move forward to the next event.
	 * If the next event can be read synchronously, the result is unblocked, else the caller has to wait for it.
	 */
	public abstract SynchronizationPoint<Exception> next();
	
	/** Shortcut to move forward to the next START_ELEMENT. */
	public ISynchronizationPoint<Exception> nextStartElement() {
		ISynchronizationPoint<Exception> next = next();
		if (next.isUnblocked()) {
			if (next.hasError()) return next;
			if (Type.START_ELEMENT.equals(event.type)) return next;
			return nextStartElement();
		}
		SynchronizationPoint<Exception> sp = new SynchronizationPoint<>();
		next.listenInline(
			() -> {
				if (Type.START_ELEMENT.equals(event.type)) {
					sp.unblock();
					return;
				}
				new Next(sp) {
					@Override
					protected void onNext() {
						if (Type.START_ELEMENT.equals(event.type)) sp.unblock();
						else nextStartElement().listenInline(sp);
					}
				}.start();
			},
			sp
		);
		return sp;
	}
	
	/** Go to the next inner element. The result is false if the parent element has been closed. */
	public AsyncWork<Boolean, Exception> nextInnerElement(ElementContext parent) {
		if (event.context.isEmpty())
			return new AsyncWork<>(Boolean.FALSE, null);
		if (Type.START_ELEMENT.equals(event.type) && event.context.getFirst() == parent && event.isClosed)
			return new AsyncWork<>(Boolean.FALSE, null);
		if (Type.END_ELEMENT.equals(event.type) && event.context.getFirst() == parent)
			return new AsyncWork<>(Boolean.FALSE, null);
		boolean parentPresent = false;
		for (ElementContext ctx : event.context)
			if (ctx == parent) {
				parentPresent = true;
				break;
			}
		if (!parentPresent)
			return new AsyncWork<>(null, new Exception("Invalid context: parent element " + parent.localName + " is not in the current context"));
		ISynchronizationPoint<Exception> next = next();
		do {
			if (next.isUnblocked()) {
				if (next.hasError()) return new AsyncWork<>(null, next.getError());
				if (Type.END_ELEMENT.equals(event.type)) {
					if (event.context.getFirst() == parent)
						return new AsyncWork<>(Boolean.FALSE, null);
				} else if (Type.START_ELEMENT.equals(event.type)) {
					if (event.context.size() > 1 && event.context.get(1) == parent)
						return new AsyncWork<>(Boolean.TRUE, null);
				}
				next = next();
				continue;
			}
			break;
		} while (true);
		AsyncWork<Boolean, Exception> result = new AsyncWork<>();
		ISynchronizationPoint<Exception> n = next;
		next.listenInline(
			() -> {
				if (n.hasError()) result.error(n.getError());
				else if (Type.END_ELEMENT.equals(event.type)) {
					if (event.context.getFirst() == parent)
						result.unblockSuccess(Boolean.FALSE);
				} else if (Type.START_ELEMENT.equals(event.type)) {
					if (event.context.size() > 1 && event.context.get(1) == parent)
						result.unblockSuccess(Boolean.TRUE);
				} else new ParsingTask(() -> {
					nextInnerElement(parent).listenInline(result);
				}).start();
			}, result
		);
		return result;
	}
	
	/** Go to the next inner element having the given name. The result is false if the parent element has been closed. */
	public AsyncWork<Boolean, Exception> nextInnerElement(ElementContext parent, String childName) {
		AsyncWork<Boolean, Exception> next = nextInnerElement(parent);
		do {
			if (next.isUnblocked()) {
				if (next.hasError()) return next;
				if (!next.getResult().booleanValue()) return next;
				if (event.text.equals(childName)) return next;
				next = nextInnerElement(parent);
				continue;
			}
			break;
		} while (true);
		AsyncWork<Boolean, Exception> result = new AsyncWork<>();
		AsyncWork<Boolean, Exception> n = next;
		next.listenInline(
			() -> {
				if (n.hasError()) result.error(n.getError());
				else if (!n.getResult().booleanValue()) result.unblockSuccess(Boolean.FALSE);
				else if (event.text.equals(childName)) result.unblockSuccess(Boolean.TRUE);
				else new ParsingTask(() -> {
					nextInnerElement(parent, childName).listenInline(result);
				}).start();
			}, result
		);
		return result;
	}
	
	/** Read inner text and close element. */
	public AsyncWork<UnprotectedStringBuffer, Exception> readInnerText() {
		if (!Type.START_ELEMENT.equals(event.type))
			return new AsyncWork<>(null, new Exception("Invalid call of readInnerText: it must be called on a start element"));
		if (event.isClosed)
			return new AsyncWork<>(new UnprotectedStringBuffer(), null);
		UnprotectedStringBuffer innerText = new UnprotectedStringBuffer();
		AsyncWork<UnprotectedStringBuffer, Exception> result = new AsyncWork<>();
		readInnerText(innerText, result);
		return result;
	}
	
	private void readInnerText(UnprotectedStringBuffer innerText, AsyncWork<UnprotectedStringBuffer, Exception> result) {
		ISynchronizationPoint<Exception> next = next();
		do {
			if (next.isUnblocked()) {
				if (next.hasError()) {
					result.error(next.getError());
					return;
				}
				if (Type.COMMENT.equals(event.type)) {
					next = next();
					continue;
				}
				if (Type.TEXT.equals(event.type)) {
					innerText.append(event.text);
					next = next();
					continue;
				}
				if (Type.START_ELEMENT.equals(event.type)) {
					if (event.isClosed) {
						next = next();
						continue;
					}
					closeElement().listenAsync(new ParsingTask(() -> {
						readInnerText(innerText, result);
					}), result);
					return;
				}
				if (Type.END_ELEMENT.equals(event.type)) {
					result.unblockSuccess(innerText);
					return;
				}
				next = next();
				continue;
			}
			break;
		} while (true);
		next.listenInline(() -> {
			if (Type.START_ELEMENT.equals(event.type)) {
				if (event.isClosed) {
					new ParsingTask(() -> { readInnerText(innerText, result); }).start();
					return;
				}
				closeElement().listenAsync(new ParsingTask(() -> {
					readInnerText(innerText, result);
				}), result);
				return;
			}
			if (Type.COMMENT.equals(event.type)) {
				new ParsingTask(() -> { readInnerText(innerText, result); }).start();
				return;
			}
			if (Type.TEXT.equals(event.type)) {
				innerText.append(event.text);
				new ParsingTask(() -> { readInnerText(innerText, result); }).start();
				return;
			}
			if (Type.END_ELEMENT.equals(event.type)) {
				result.unblockSuccess(innerText);
				return;
			}
			new ParsingTask(() -> { readInnerText(innerText, result); }).start();
		}, result);
	}
	
	/** Go the the END_ELEMENT event corresponding to the current START_ELEMENT (must be called with a current event to START_ELEMENT). */
	public ISynchronizationPoint<Exception> closeElement() {
		if (!Type.START_ELEMENT.equals(event.type))
			return new SynchronizationPoint<>(new Exception("Invalid call of closeElement: it must be called on a start element"));
		if (event.isClosed)
			return new SynchronizationPoint<>(true);
		ElementContext ctx = event.context.getFirst();
		return closeElement(ctx);
	}
	
	public ISynchronizationPoint<Exception> closeElement(ElementContext ctx) {
		ISynchronizationPoint<Exception> next = next();
		do {
			if (!next.isUnblocked()) break;
			if (next.hasError()) return next;
			if (Type.END_ELEMENT.equals(event.type)) {
				if (event.context.getFirst() == ctx)
					return next;
			}
			next = next();
		} while (true);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		ISynchronizationPoint<Exception> n = next;
		next.listenInline(() -> {
			if (n.hasError()) {
				result.error(n.getError());
				return;
			}
			if (Type.END_ELEMENT.equals(event.type)) {
				if (event.context.getFirst() == ctx) {
					result.unblock();
					return;
				}
			}
			new ParsingTask(() -> { closeElement(ctx).listenInline(result); }).start();
		}, result);
		return result;
	}

	/** Move forward until an element with the given name is found (whatever its depth).
	 */
	public ISynchronizationPoint<Exception> searchElement(String elementName) {
		ISynchronizationPoint<Exception> next = next();
		do {
			if (!next.isUnblocked()) break;
			if (next.hasError()) return next;
			if (Type.START_ELEMENT.equals(event.type) && event.text.equals(elementName)) return next;
			next = next();
		} while (true);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		ISynchronizationPoint<Exception> n = next;
		next.listenInline(() -> {
			if (n.hasError()) result.error(n.getError());
			else if (Type.START_ELEMENT.equals(event.type) && event.text.equals(elementName)) result.unblock();
			else new ParsingTask(() -> { searchElement(elementName).listenInline(result); }).start();
		}, result);
		return result;
	}
	
	/** Go successively into the given elements. */
	public ISynchronizationPoint<Exception> goInto(ElementContext rootContext, String... innerElements) {
		return goInto(rootContext, 0, innerElements);
	}
	
	private ISynchronizationPoint<Exception> goInto(ElementContext parent, int i, String... innerElements) {
		ISynchronizationPoint<Exception> next = nextInnerElement(parent, innerElements[i]);
		do {
			if (!next.isUnblocked()) break;
			if (next.hasError()) return next;
			i++;
			if (i == innerElements.length) return next;
			parent = event.context.getFirst();
			next = nextInnerElement(parent, innerElements[i]);
		} while (true);
		SynchronizationPoint<Exception> result = new SynchronizationPoint<>();
		ISynchronizationPoint<Exception> n = next;
		int ii = i;
		next.listenInline(() -> {
			if (n.hasError()) result.error(n.getError());
			else if (ii == innerElements.length - 1) result.unblock();
			else new ParsingTask(() -> { goInto(event.context.getFirst(), ii + 1, innerElements).listenInline(result); }).start();
		}, result);
		return result;
	}
	
	/** Read all inner elements with their text, and return a mapping with the element's name as key and inner text as value. */
	public AsyncWork<Map<String,String>, Exception> readInnerElementsText() {
		AsyncWork<Map<String,String>, Exception> result = new AsyncWork<>();
		Map<String, String> texts = new HashMap<>();
		readInnerElementsText(event.context.getFirst(), texts, result);
		return result;
	}
	
	private void readInnerElementsText(ElementContext parent, Map<String, String> texts, AsyncWork<Map<String,String>, Exception> result) {
		AsyncWork<Boolean, Exception> next = nextInnerElement(parent);
		do {
			if (!next.isUnblocked()) break;
			if (next.hasError()) {
				result.error(next.getError());
				return;
			}
			if (!next.getResult().booleanValue()) {
				result.unblockSuccess(texts);
				return;
			}
			String name = event.text.asString();
			AsyncWork<UnprotectedStringBuffer, Exception> read = readInnerText();
			if (read.isUnblocked()) {
				if (read.hasError()) {
					result.error(read.getError());
					return;
				}
				texts.put(name, read.getResult().asString());
				next = nextInnerElement(parent);
				continue;
			}
			read.listenInline((value) -> {
				texts.put(name,  value.asString());
				new ParsingTask(() -> { readInnerElementsText(parent, texts, result); }).start();
			}, result);
			return;
		} while (true);
		next.listenInline(() -> {
			String name = event.text.asString();
			new ParsingTask(() -> {
				AsyncWork<UnprotectedStringBuffer, Exception> read = readInnerText();
				if (read.isUnblocked()) {
					if (read.hasError()) {
						result.error(read.getError());
						return;
					}
					texts.put(name, read.getResult().asString());
					readInnerElementsText(parent, texts, result);
					return;
				}
				read.listenInline((value) -> {
					texts.put(name,  value.asString());
					new ParsingTask(() -> { readInnerElementsText(parent, texts, result); }).start();
				}, result);
			}).start();
		}, result);
	}

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
	
	protected class Next extends ParsingTask {
		public Next(SynchronizationPoint<Exception> sp) {
			super(null);
			this.sp = sp;
		}
		
		protected SynchronizationPoint<Exception> sp;
		
		@Override
		public Void run() {
			ISynchronizationPoint<Exception> next = next();
			if (next.isUnblocked()) {
				if (next.hasError()) sp.error(next.getError());
				else onNext();
				return null;
			}
			next.listenAsync(new Task.Cpu<Void, NoException>("Parse XML", getPriority()) {
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
