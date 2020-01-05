package net.lecousin.framework.xml;

import java.io.Serializable;

import net.lecousin.framework.exception.LocalizableException;
import net.lecousin.framework.io.text.ICharacterStream;
import net.lecousin.framework.locale.ILocalizableString;
import net.lecousin.framework.locale.LocalizableString;
import net.lecousin.framework.locale.LocalizableStringBuffer;

/** XML parsing error. */
public class XMLException extends LocalizableException {
	
	public static final String LOCALIZED_NAMESPACE_XML_ERROR = "lc.xml.error";
	public static final String LOCALIZED_MESSAGE_NOT_XML = "Not an XML file";
	public static final String LOCALIZED_MESSAGE_INVALID_XML = "Invalid XML";
	public static final String LOCALIZED_MESSAGE_UNEXPECTED_CHARACTER = "Unexpected character";
	public static final String LOCALIZED_MESSAGE_UNEXPECTED_END = "Unexpected end";
	public static final String LOCALIZED_MESSAGE_UNEXPECTED_ELEMENT = "Unexpected element";
	public static final String LOCALIZED_MESSAGE_EXPECTED_CHARACTER = "Expected character";
	public static final String LOCALIZED_MESSAGE_IN_XML_DOCUMENT = "in XML document";
	public static final String LOCALIZED_MESSAGE_IN_INTERNAL_SUBSET = "in internal subset declaration";

	private static final long serialVersionUID = -963187033082725980L;

	/** Constructor. */
	public XMLException(ICharacterStream.Readable stream, Object context, String message, Serializable... values) {
		super(get(stream, context, new LocalizableString(LOCALIZED_NAMESPACE_XML_ERROR, message, values)));
	}
	
	/** Constructor. */
	public XMLException(ICharacterStream.Readable stream, Object context, ILocalizableString message) {
		super(get(stream, context, message));
	}

	/** Constructor. */
	public XMLException(ICharacterStream.Readable stream, Object context, ILocalizableString... messages) {
		super(get(stream, context, get(messages)));
	}
	
	private static ILocalizableString get(ICharacterStream.Readable stream, Object context, ILocalizableString message) {
		LocalizableStringBuffer result = new LocalizableStringBuffer(message);
		if (stream instanceof ICharacterStream.Readable.PositionInText) {
			result.add(" ");
			result.add(new LocalizableString(LOCALIZED_NAMESPACE_XML_ERROR, "at"));
			result.add(" ");
			result.add(new LocalizableString(LOCALIZED_NAMESPACE_XML_ERROR, "line"));
			result.add(" " + ((ICharacterStream.Readable.PositionInText)stream).getLine() + " ");
			result.add(new LocalizableString(LOCALIZED_NAMESPACE_XML_ERROR, "character"));
			result.add(" " + ((ICharacterStream.Readable.PositionInText)stream).getPositionInLine());
		}
		if (context != null) {
			result.add(" (");
			result.add(context.toString());
			result.add(")");
		}
		return result;
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
