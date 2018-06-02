package net.lecousin.framework.application.libraries.artifacts.maven;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.synch.AsyncWork;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFromURL;
import net.lecousin.framework.log.Logger;
import net.lecousin.framework.xml.XMLStreamEvents.ElementContext;
import net.lecousin.framework.xml.XMLStreamEvents.Event.Type;
import net.lecousin.framework.xml.XMLStreamReader;

/**
 * Maven remote repository.
 */
public class MavenRemoteRepository implements MavenRepository {

	/** Constructor. */
	public MavenRemoteRepository(String url, boolean releasesEnabled, boolean snapshotsEnabled) {
		if (!url.endsWith("/"))
			url += "/";
		this.url = url;
		this.releasesEnabled = releasesEnabled;
		this.snapshotsEnabled = snapshotsEnabled;
		this.logger = LCCore.getApplication().getLoggerFactory().getLogger(MavenRemoteRepository.class);
	}
	
	private String url;
	private boolean releasesEnabled;
	private boolean snapshotsEnabled;
	private Logger logger;

	@Override
	public boolean isReleasesEnabled() {
		return releasesEnabled;
	}
	
	@Override
	public boolean isSnapshotsEnabled() {
		return snapshotsEnabled;
	}
	
	@Override
	public boolean isSame(String url, boolean releasesEnabled, boolean snapshotsEnabled) {
		if (releasesEnabled != this.releasesEnabled)
			return false;
		if (snapshotsEnabled != this.snapshotsEnabled)
			return false;
		if (!url.equals(this.url))
			return false;
		return true;
	}
	
	@SuppressWarnings("resource")
	@Override
	public AsyncWork<List<String>, NoException> getAvailableVersions(String groupId, String artifactId, byte priority) {
		String path = groupId.replace('.', '/') + '/' + artifactId + "/maven-metadata.xml";
		if (logger.info())
			logger.info("Downloading " + url + path);
		IO.Readable io;
		try {
			IOProvider p = IOProviderFromURL.getInstance().get(new URL(url + path));
			if (p == null || !(p instanceof IOProvider.Readable))
				return new AsyncWork<>(null, null);
			io = ((IOProvider.Readable)p).provideIOReadable(priority);
		} catch (Exception e) {
			if (logger.error())
				logger.error("Unable to get IOProvider for " + url + path, e);
			return new AsyncWork<>(null, null);
		}
		IO.Readable.Buffered bio;
		if (io instanceof IO.Readable.Buffered)
			bio = (IO.Readable.Buffered)io;
		else
			bio = new SimpleBufferedReadable(io, 8192);
		AsyncWork<XMLStreamReader, Exception> start = XMLStreamReader.start(bio, 5000);
		AsyncWork<List<String>, NoException> result = new AsyncWork<>();
		start.listenAsync(new Task.Cpu.FromRunnable("Read maven-metadata.xml", priority, () -> {
			if (!start.isSuccessful()) {
				if (logger.error())
					logger.error("Error loading " + url + path, start.getError());
				result.unblockSuccess(null);
				return;
			}
			try {
				XMLStreamReader xml = start.getResult();
				while (!Type.START_ELEMENT.equals(xml.event.type)) xml.next();
				if (!xml.goInto(xml.event.context.getFirst(), "versioning", "versions")) {
					if (logger.error())
						logger.error(url + path + " does not contain element versioning/versions");
					result.unblockSuccess(null);
					return;
				}
				ElementContext parent = xml.event.context.getFirst();
				List<String> versions = new LinkedList<>();
				while (xml.nextInnerElement(parent, "version")) {
					versions.add(xml.readInnerText().asString());
				}
				result.unblockSuccess(versions);
			} catch (Exception e) {
				if (logger.error())
					logger.error("Error parsing " + url + path, e);
				result.unblockSuccess(null);
			}
		}), true);
		result.listenInline(() -> { io.closeAsync(); });
		return result;
	}
	
	@Override
	public AsyncWork<MavenPOM, Exception> load(
		String groupId, String artifactId, String version, MavenPOMLoader pomLoader, byte priority
	) {
		String path = groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version + ".pom";
		if (logger.info())
			logger.info("Downloading " + url + path);
		try {
			return pomLoader.loadPOM(new URL(url + path), priority);
		} catch (Exception e) {
			return new AsyncWork<>(null, e);
		}
	}
	
	@Override
	public String toString() {
		return "remote maven repository (" + url + ")";
	}
}
