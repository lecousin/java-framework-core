package net.lecousin.framework.locale;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.JoinPoint;
import net.lecousin.framework.exception.NoException;

/**
 * List of objects to concatenate to form a string, in which any object implementing ILocalizableString will be localized.
 */
public class LocalizableStringBuffer implements ILocalizableString {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public LocalizableStringBuffer(Serializable... list) {
		for (int i = 0; i < list.length; ++i)
			this.list.add(list[i]);
	}
	
	private LinkedList<Serializable> list = new LinkedList<>();
	
	/** Append the given object. */
	public void add(Serializable string) {
		list.add(string);
	}
	
	@Override
	public AsyncSupplier<String, NoException> localize(String[] languageTag) {
		JoinPoint<NoException> jp = new JoinPoint<>();
		List<AsyncSupplier<String, NoException>> localizations = new LinkedList<>();
		for (Serializable o : this.list)
			if (o instanceof ILocalizableString) {
				AsyncSupplier<String, NoException> l = ((ILocalizableString)o).localize(languageTag);
				jp.addToJoin(l);
				localizations.add(l);
			}
		AsyncSupplier<String, NoException> result = new AsyncSupplier<>();
		jp.start();
		jp.onDone(() -> {
			Iterator<AsyncSupplier<String, NoException>> it = localizations.iterator();
			StringBuilder s = new StringBuilder();
			for (Serializable o : LocalizableStringBuffer.this.list) {
				if (o instanceof String)
					s.append((String)o);
				else if (o instanceof ILocalizableString)
					s.append(it.next().getResult());
				else
					s.append(o);
			}
			result.unblockSuccess(s.toString());
		});
		return result;
	}
	
	@Override
	public String localizeSync(String[] languageTag) {
		StringBuilder s = new StringBuilder();
		for (Serializable o : list) {
			if (o instanceof String)
				s.append((String)o);
			else if (o instanceof ILocalizableString)
				s.append(((ILocalizableString)o).localizeSync(languageTag));
			else
				s.append(o);
		}
		return s.toString();
	}
}
