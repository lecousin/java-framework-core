package net.lecousin.framework.locale;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.collections.ArrayUtil;
import net.lecousin.framework.concurrent.async.Async;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.concurrent.async.IAsync;
import net.lecousin.framework.concurrent.threads.Task;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.buffering.PreBufferedReadable;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFromPathUsingClassloader;
import net.lecousin.framework.io.text.BufferedReadableCharacterStream;
import net.lecousin.framework.io.text.PropertiesReader;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.memory.IMemoryManageable;
import net.lecousin.framework.memory.MemoryManager;
import net.lecousin.framework.text.CharArrayStringBuffer;
import net.lecousin.framework.util.ClassUtil;
import net.lecousin.framework.util.ObjectUtil;

/**
 * Properties loaded from localized properties files, organized by namespace.
 */
public class LocalizedProperties implements IMemoryManageable {

	/** Constructor. */
	public LocalizedProperties(Application application) {
		logger = application.getLoggerFactory().getLogger(LocalizedProperties.class);
		MemoryManager.register(this);
		
		registerNamespaceFrom(LocalizedProperties.class, "b", "b");
		registerNamespaceFrom(LocalizedProperties.class, "languages", "languages");
	}
	
	private Logger logger;
	private Map<String, Namespace> namespaces = new HashMap<>();
	
	private static class Namespace {
		private ClassLoader classLoader;
		private String path;
		private IAsync<IOException> loading;
		private Language[] languages;
		
		private static class Language {
			private String[] tag;
			private Language parent = null;
			private Map<String, String> properties = null;
			private Async<IOException> loading = null;
			private long lastUsage = 0;
		}
	}
	
	/** Register the path on which localized properties can be found for a namespace.
	 * If the namespace already exists, the new path will override the previous one.
	 */
	public IAsync<IOException> registerNamespace(String namespace, String path, ClassLoader classLoader) {
		Namespace ns;
		Async<IOException> sp = new Async<>();
		synchronized (namespaces) {
			ns = namespaces.get(namespace);
			if (ns == null) {
				ns = new Namespace();
				ns.loading = sp;
				namespaces.put(namespace, ns);
			} else {
				sp.error(new IOException("Namespace already registered: " + namespace));
				return sp;
			}
		}
		ns.classLoader = classLoader;
		ns.path = path;
		IOProvider.Readable provider = new IOProviderFromPathUsingClassloader(classLoader).get(path + ".languages");
		IO.Readable input;
		try { 
			input = provider == null ? null : provider.provideIOReadable(Task.Priority.RATHER_IMPORTANT);
			if (input == null) throw new IOException("no file");
		} catch (IOException e) {
			sp.error(new IOException("Localized properties for namespace " + namespace
				+ " cannot be loaded because the file " + path + ".languages does not exist", e));
			logger.error(sp.getError().getMessage());
			return sp;
		}
		AsyncSupplier<CharArrayStringBuffer, IOException> read = IOUtil.readFullyAsString(
			input, StandardCharsets.US_ASCII, Task.Priority.RATHER_IMPORTANT);
		Namespace toLoad = ns;
		read.thenStart("Read localized properties namespace file", Task.Priority.RATHER_IMPORTANT, () -> {
			List<Namespace.Language> languages = parseLanguages(read.getResult());
			languages.sort(languageComparator);
			toLoad.languages = languages.toArray(new Namespace.Language[languages.size()]);
			// set parents
			for (int i = 0; i < toLoad.languages.length; ++i) {
				// they are ordered, so the parent is after
				int nbI = toLoad.languages[i].tag.length;
				for (int j = i + 1; j < toLoad.languages.length; ++j) {
					int nbJ = toLoad.languages[j].tag.length;
					if (nbJ >= nbI) continue;
					if (ArrayUtil.equals(toLoad.languages[i].tag, 0, toLoad.languages[j].tag, 0, nbJ))
						toLoad.languages[i].parent = toLoad.languages[j];
					break;
				}
			}
			sp.unblock();
			logger.info("Namespace " + namespace + " loaded with " + languages.size() + " languages from " + path);
			return null;
		}, sp);
		input.closeAfter(sp);
		sp.onError(error -> logger.error("Error loading localized properties namespace file " + path + ".languages", error));
		return sp;
	}
	
	private static List<Namespace.Language> parseLanguages(CharArrayStringBuffer str) {
		List<Namespace.Language> languages = new LinkedList<>();
		for (CharArrayStringBuffer s : str.split(',')) {
			s.trim().toLowerCase();
			if (s.isEmpty()) continue;
			Namespace.Language l = new Namespace.Language();
			List<CharArrayStringBuffer> list = s.split('-');
			l.tag = new String[list.size()];
			int i = 0;
			for (CharArrayStringBuffer us : list) l.tag[i++] = us.asString();
			languages.add(l);
		}
		return languages;
	}
	
	private static final Comparator<Namespace.Language> languageComparator = (l1, l2) -> {
		/* Order:
		 *  tags are alphabetically ordered
		 *  but tags xx-yy are before tag xx
		 */
		int i = 0;
		do {
			if (i == l1.tag.length) return 1;
			if (i == l2.tag.length) return -1;
			int c = l1.tag[i].compareTo(l2.tag[i]);
			if (c != 0) return c;
			i++;
		} while (true);
	};
	
	/** Register the path on which localized properties can be found for a namespace.
	 * If the namespace already exists, the new path will override the previous one.
	 * The path is calculated by taking the package name of the given calss, then appending the subPath.
	 */
	public IAsync<IOException> registerNamespaceFrom(Class<?> cl, String namespace, String subPath) {
		return registerNamespace(namespace, ClassUtil.getPackageName(cl).replace('.', '/') + '/' + subPath, cl.getClassLoader());
	}
	
	public Set<String> getDeclaredNamespaces() {
		return namespaces.keySet();
	}
	
	private void load(Namespace ns, Namespace.Language lang) {
		String path = ns.path + '.' + String.join("-", lang.tag);
		IOProvider.Readable provider = new IOProviderFromPathUsingClassloader(ns.classLoader).get(path);
		IO.Readable input;
		try { input = provider != null ? provider.provideIOReadable(Task.Priority.RATHER_IMPORTANT) : null; }
		catch (IOException e) {
			lang.loading.error(new IOException("Localized properties file " + path + " does not exist", e));
			logger.error(lang.loading.getError().getMessage());
			return;
		}
		if (input == null) {
			lang.loading.error(new IOException("Localized properties file " + path + " does not exist"));
			logger.error(lang.loading.getError().getMessage());
			return;
		}
		if (!(input instanceof IO.Readable.Buffered))
			input = new PreBufferedReadable(input, 4096, Task.Priority.RATHER_IMPORTANT, 4096, Task.Priority.RATHER_IMPORTANT, 16);
		IO.Readable.Buffered in = (IO.Readable.Buffered)input;
		BufferedReadableCharacterStream cs = new BufferedReadableCharacterStream(in, StandardCharsets.UTF_8, 3000, 16);
		lang.properties = new HashMap<>();
		PropertiesReader<Map<String,String>> reader = new PropertiesReader<Map<String,String>>(
			"Localized properties " + path, cs, Task.Priority.RATHER_IMPORTANT, IO.OperationType.ASYNCHRONOUS
		) {
			@Override
			protected void processProperty(CharArrayStringBuffer key, CharArrayStringBuffer value) {
				lang.properties.put(key.asString(), value.asString());
			}
			
			@Override
			protected Map<String, String> generateResult() {
				return lang.properties;
			}
		};
		reader.start().onDone(lang.loading, IO::error);
	}

	/** Localization. */
	public String localizeSync(String[] languageTag, String namespace, String key, Serializable... values) {
		try {
			return localize(languageTag, namespace, key, values).blockResult(0);
		} catch (Exception e) {
			return "";
		}
	}
	
	/** Localization. */
	public AsyncSupplier<String, NoException> localize(String[] languageTag, String namespace, String key, Serializable... values) {
		AsyncSupplier<String, NoException> result = new AsyncSupplier<>();
		Namespace ns;
		synchronized (namespaces) {
			ns = namespaces.get(namespace);
		}
		if (ns == null) {
			result.unblockSuccess("!! unknown namespace " + namespace + " !!");
			return result;
		}
		ns.loading.onDone(new Runnable() {
			@Override
			public void run() {
				if (ns.loading.hasError()) {
					result.unblockSuccess("!! error loading namespace " + namespace + " !!");
					return;
				}
				if (!ns.loading.isDone()) {
					// namespace has been overriden and is reloading
					ns.loading.onDone(this);
					return;
				}
				for (Namespace.Language l : ns.languages)
					if (languageTagCompatible(l.tag, languageTag)) {
						localize(ns, l, key, values, result);
						return;
					}
				result.unblockSuccess("!! no compatible language in namespace " + namespace + "!!");
			}
		});
		return result;
	}
	
	private void localize(Namespace ns, Namespace.Language lang, String key, Serializable[] values, AsyncSupplier<String, NoException> result) {
		boolean needsLoading = false;
		synchronized (lang) {
			if (lang.loading == null) {
				lang.loading = new Async<>();
				needsLoading = true;
			}
		}
		lang.loading.onDone(() -> {
			synchronized (lang) {
				String content = null;
				if (!lang.loading.hasError())
					content = lang.properties.get(key.toLowerCase());
				if (content != null) {
					localize(lang.tag, key, content, values, result);
					lang.lastUsage = System.currentTimeMillis();
					return;
				}
			}
			if (lang.parent != null)
				localize(ns, lang.parent, key, values, result);
			else
				result.unblockSuccess("!! missing key " + key + " !!");
		});
		if (needsLoading)
			load(ns, lang);
	}
	
	private static void localize(
		String[] languageTag, String key, String content, Serializable[] values, AsyncSupplier<String, NoException> result
	) {
		for (int i = 0; i < values.length; ++i)
			if (values[i] instanceof ILocalizableString) {
				AsyncSupplier<String, NoException> l = ((ILocalizableString)values[i]).localize(languageTag);
				Serializable[] newValues = new Serializable[values.length];
				System.arraycopy(values, 0, newValues, 0, values.length);
				final int ii = i;
				l.thenStart("Localization", Task.Priority.NORMAL, () -> {
					newValues[ii] = l.getResult();
					localize(languageTag, key, content, newValues, result);
					return null;
				}, true);
				return;
			}
		result.unblockSuccess(setCase(replaceValues(content, values), key));
	}
	
	private static String replaceValues(String s, Serializable[] values) {
		for (int i = 0; i < values.length; ++i)
			s = s.replace("{" + i + "}", ObjectUtil.toString(values[i]));
		return s;
	}
	
	private static String setCase(String text, String key) {
		if (Character.isUpperCase(key.charAt(0)))
			text = Character.toUpperCase(text.charAt(0)) + text.substring(1);
		// TODO
		return text;
	}

	private static boolean languageTagCompatible(String[] tag, String[] requested) {
		if (tag.length > requested.length) return false;
		return ArrayUtil.equals(tag, 0, requested, 0, tag.length);
	}

	// IMemoryManageable
	
	@Override
	public String getDescription() {
		return "Localized properties";
	}

	@Override
	public List<String> getItemsDescription() {
		ArrayList<String> items = new ArrayList<>(namespaces.size());
		synchronized (namespaces) {
			for (Map.Entry<String, Namespace> ns : namespaces.entrySet()) {
				int loaded = 0;
				if (ns.getValue().languages != null)
					for (Namespace.Language lang : ns.getValue().languages)
						if (lang.loading != null) loaded++;
				items.add("Localized properties for namespace " + ns.getKey() + " (" + loaded + " language(s) loaded)");
			}
		}
		return items;
	}

	@Override
	public void freeMemory(FreeMemoryLevel level) {
		long maxIdle;
		switch (level) {
		default:
		case EXPIRED_ONLY:
			maxIdle = 10L * 60 * 1000;
			break;
		case LOW:
			maxIdle = 2L * 60 * 1000;
			break;
		case MEDIUM:
			maxIdle = 45L * 1000;
			break;
		case URGENT:
			maxIdle = 5L * 1000;
			break;
		}
		synchronized (namespaces) {
			for (Namespace ns : namespaces.values()) {
				if (ns.languages != null)
					for (Namespace.Language lang : ns.languages) {
						synchronized (lang) {
							if (lang.loading == null) continue;
							if (!lang.loading.isDone()) continue;
							if (lang.properties != null && System.currentTimeMillis() - lang.lastUsage > maxIdle) {
								lang.loading = null;
								lang.properties = null;
								lang.lastUsage = 0;
							}
						}
					}
			}
		}
	}
	
	/** Return the list of available languages. */
	public Collection<String> getAvailableLanguageCodes() {
		List<String> avail = new LinkedList<>();
		boolean first = true;
		synchronized (namespaces) {
			for (Namespace ns : namespaces.values()) {
				if (ns.languages != null) {
					List<String> found = new LinkedList<>();
					for (Namespace.Language l : ns.languages) {
						String tag = String.join("-", l.tag);
						if (first) avail.add(tag);
						else found.add(tag);
					}
					if (first) first = false;
					else for (Iterator<String> it = avail.iterator(); it.hasNext(); ) {
						if (!found.contains(it.next()))
							it.remove();
					}
				}
			}
		}
		return avail;
	}
	
	/** Return the languages declared for the given namespace. */
	public Set<String> getNamespaceLanguages(String namespace) {
		Set<String> list = new HashSet<>();
		Namespace ns = namespaces.get(namespace);
		if (ns != null && ns.languages != null)
			for (Namespace.Language l : ns.languages)
				list.add(String.join("-", l.tag));
		return list;
	}
	
	/** Return the localizable properties for the given namespace and language. */
	public AsyncSupplier<Map<String, String>, IOException> getNamespaceContent(String namespace, String[] languageTag) {
		AsyncSupplier<Map<String, String>, IOException> result = new AsyncSupplier<>();
		Namespace ns;
		synchronized (namespaces) {
			ns = namespaces.get(namespace);
		}
		if (ns == null) {
			result.error(new IOException("Unknown namespace " + namespace));
			return result;
		}
		ns.loading.onDone(new Runnable() {
			@Override
			public void run() {
				if (!ns.loading.isDone()) {
					// namespace has been overriden and is reloading
					ns.loading.onDone(this, result);
					return;
				}
				for (Namespace.Language l : ns.languages)
					if (ArrayUtil.equals(l.tag, languageTag)) {
						boolean needsLoading = false;
						synchronized (l) {
							if (l.loading == null) {
								l.loading = new Async<>();
								needsLoading = true;
							}
						}
						l.loading.onDone(() -> {
							synchronized (l) {
								result.unblockSuccess(new HashMap<>(l.properties));
							}
						}, result);
						if (needsLoading)
							load(ns, l);
						return;
					}
				result.error(new IOException("Language not found in namespace " + namespace));
			}
		}, result);
		return result;
	}
	
}
