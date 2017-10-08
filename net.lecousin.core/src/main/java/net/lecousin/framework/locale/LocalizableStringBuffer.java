package net.lecousin.framework.locale;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.exception.NoException;

/**
 * List of objects to concatenate to form a string, in which any object implementing ILocalizableString will be localized.
 */
public class LocalizableStringBuffer implements ILocalizableString {

	/** Constructor. */
	public LocalizableStringBuffer(Object... list) {
		for (int i = 0; i < list.length; ++i)
			this.list.add(list[i]);
	}
	
	private LinkedList<Object> list = new LinkedList<>();
	
	/** Append the given object. */
	public void add(Object string) {
		list.add(string);
	}
	
	@Override
	public AsyncWork<String, NoException> localize(String[] languageTag) {
		JoinPoint<NoException> jp = new JoinPoint<>();
		List<AsyncWork<String, NoException>> list = new LinkedList<>();
		for (Object o : list)
			if (o instanceof ILocalizableString) {
				AsyncWork<String, NoException> l = ((ILocalizableString)o).localize(languageTag);
				jp.addToJoin(l);
				list.add(l);
			}
		AsyncWork<String, NoException> result = new AsyncWork<>();
		jp.start();
		jp.listenInline(new Runnable() {
			@Override
			public void run() {
				Iterator<AsyncWork<String, NoException>> it = list.iterator();
				StringBuffer s = new StringBuffer();
				for (Object o : list) {
					if (o instanceof String)
						s.append((String)o);
					else if (o instanceof ILocalizableString)
						s.append(it.next().getResult());
					else
						s.append(o);
				}
				result.unblockSuccess(s.toString());
			}
		});
		return result;
	}
	
	@Override
	public String localizeSync(String[] languageTag) {
		StringBuffer s = new StringBuffer();
		for (Object o : list) {
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
