package net.lecousin.framework.locale;

import java.util.ArrayList;
import java.util.List;

import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.concurrent.synch.JoinPoint;
import net.lecousin.framework.exception.NoException;

/**
 * Several localizable strings joined by a separator.
 */
public class CompositeLocalizable implements ILocalizableString {

	private static final long serialVersionUID = 1L;

	/** Constructor. */
	public CompositeLocalizable(String sep, ILocalizableString... elements) {
		this.sep = sep;
		this.elements = elements;
	}
	
	private String sep;
	private ILocalizableString[] elements;
	
	@Override
	public AsyncWork<String, NoException> localize(String[] languageTag) {
		JoinPoint<NoException> jp = new JoinPoint<>();
		List<AsyncWork<String, NoException>> list = new ArrayList<>(elements.length);
		for (ILocalizableString s : elements) {
			AsyncWork<String, NoException> l = s.localize(languageTag);
			jp.addToJoin(l);
			list.add(l);
		}
		AsyncWork<String, NoException> result = new AsyncWork<>();
		jp.start();
		jp.listenInline(() -> {
			StringBuilder s = new StringBuilder();
			for (AsyncWork<String, NoException> es : list) {
				if (s.length() > 0) s.append(sep);
				s.append(es.getResult());
			}
			result.unblockSuccess(s.toString());
		});
		return result;
	}
	
	@Override
	public String localizeSync(String[] languageTag) {
		StringBuilder s = new StringBuilder();
		for (ILocalizableString ls : elements) {
			if (s.length() > 0) s.append(sep);
			s.append(ls.localizeSync(languageTag));
		}
		return s.toString();
	}
	
}
