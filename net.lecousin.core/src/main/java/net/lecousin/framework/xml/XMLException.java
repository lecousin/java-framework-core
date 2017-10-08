package net.lecousin.framework.xml;

import net.lecousin.framework.exception.LocalizableException;
import net.lecousin.framework.locale.ILocalizableString;
import net.lecousin.framework.locale.LocalizableString;
import net.lecousin.framework.locale.LocalizableStringBuffer;
import net.lecousin.framework.util.Pair;

/** XML parsing error. */
public class XMLException extends LocalizableException {

	private static final long serialVersionUID = -963187033082725980L;

	/** Constructor. */
	public XMLException(Pair<Integer,Integer> pos, String message, Object... values) {
		super(get(pos, new LocalizableString("lc.xml.error", message, values)));
	}
	
	/** Constructor. */
	public XMLException(Pair<Integer,Integer> pos, ILocalizableString message) {
		super(get(pos, message));
	}

	/** Constructor. */
	public XMLException(Pair<Integer,Integer> pos, ILocalizableString... messages) {
		super(get(pos, get(messages)));
	}
	
	private static ILocalizableString get(Pair<Integer,Integer> pos, ILocalizableString message) {
		if (pos == null) return message;
		return new LocalizableStringBuffer(
			message, " ", new LocalizableString("lc.xml.error", "at"), " ",
			new LocalizableString("lc.xml.error", "line"), " " + pos.getValue1() + " ",
			new LocalizableString("lc.xml.error", "character"), " " + pos.getValue2());
	}
	
	private static ILocalizableString get(ILocalizableString... messages) {
		LocalizableStringBuffer res = new LocalizableStringBuffer();
		for (int i = 0; i < messages.length; ++i) {
			if (i > 0) res.add(" ");
			res.add(messages[i]);
		}
		return res;
	}
	
}
