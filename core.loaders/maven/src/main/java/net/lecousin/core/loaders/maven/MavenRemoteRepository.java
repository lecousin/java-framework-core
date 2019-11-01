package net.lecousin.core.loaders.maven;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.application.libraries.LibraryManagementException;
import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.async.AsyncSupplier;
import net.lecousin.framework.exception.NoException;
import net.lecousin.framework.io.FileIO;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOUtil;
import net.lecousin.framework.io.TemporaryFiles;
import net.lecousin.framework.io.buffering.SimpleBufferedReadable;
import net.lecousin.framework.io.provider.IOProvider;
import net.lecousin.framework.io.provider.IOProviderFromURI;
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
		return
			releasesEnabled == this.releasesEnabled &&
			snapshotsEnabled == this.snapshotsEnabled &&
			url.equals(this.url);
	}
	
	private IO.Readable download(String path, byte priority) {
		if (logger.info())
			logger.info("Downloading " + url + path);
		IO.Readable io;
		try {
			IOProvider p = IOProviderFromURI.getInstance().get(new URI(url + path));
			if (!(p instanceof IOProvider.Readable))
				return null;
			io = ((IOProvider.Readable)p).provideIOReadable(priority);
		} catch (Exception e) {
			if (logger.error())
				logger.error("Unable to get IOProvider for " + url + path, e);
			return null;
		}
		return io;
	}
	
	@Override
	public AsyncSupplier<List<String>, NoException> getAvailableVersions(String groupId, String artifactId, byte priority) {
		String path = groupId.replace('.', '/') + '/' + artifactId + "/maven-metadata.xml";
		IO.Readable io = download(path, priority);
		if (io == null) return new AsyncSupplier<>(null, null);
		IO.Readable.Buffered bio;
		if (io instanceof IO.Readable.Buffered)
			bio = (IO.Readable.Buffered)io;
		else
			bio = new SimpleBufferedReadable(io, 8192);
		AsyncSupplier<XMLStreamReader, Exception> start = XMLStreamReader.start(bio, 5000, 4, false);
		AsyncSupplier<List<String>, NoException> result = new AsyncSupplier<>();
		start.thenStart(new Task.Cpu.FromRunnable("Read maven-metadata.xml", priority, () -> {
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
		}), () -> {
			if (logger.error())
				logger.error("Error loading " + url + path, start.getError());
			result.unblockSuccess(null);
		});
		io.closeAfter(result);
		return result;
	}
	
	@Override
	public AsyncSupplier<MavenPOM, LibraryManagementException> load(
		String groupId, String artifactId, String version, MavenPOMLoader pomLoader, byte priority
	) {
		String path = groupId.replace('.', '/') + '/' + artifactId + '/' + version + '/' + artifactId + '-' + version + ".pom";
		if (logger.info())
			logger.info("Downloading " + url + path);
		try {
			return pomLoader.loadPOM(new URI(url + path), true, priority);
		} catch (Exception e) {
			return new AsyncSupplier<>(null, new MavenPOMException("Error loading POM file " + url + path, e));
		}
	}

	@Override
	public File loadFileSync(String groupId, String artifactId, String version, String classifier, String type) {
		try {
			return loadFile(groupId, artifactId, version, classifier, type, Task.PRIORITY_IMPORTANT).blockResult(0);
		} catch (Exception e) {
			return null;
		}
	}
	
	@Override
	public AsyncSupplier<File, IOException> loadFile(
		String groupId, String artifactId, String version, String classifier, String type, byte priority
	) {
		String path = groupId.replace('.', '/') + '/' + artifactId + '/' + version
			+ '/' + MavenPOM.getFilename(artifactId, version, classifier, type);
		IO.Readable io = download(path, priority);
		if (io == null) return new AsyncSupplier<>(null, null);
		AsyncSupplier<File, IOException> file = TemporaryFiles.get().createFileAsync("remote-maven", ".downloaded");
		AsyncSupplier<File, IOException> result = new AsyncSupplier<>();
		file.thenDoOrStart(f -> {
			FileIO.WriteOnly out = new FileIO.WriteOnly(f, priority);
			IOUtil.copy(io, out, -1, true, null, 0).onDone(() -> result.unblockSuccess(f), result);
		}, "Download file from maven", priority, result);
		result.onErrorOrCancel(io::closeAsync);
		return result;
	}

	
	@Override
	public String toString() {
		return "remote maven repository (" + url + ")";
	}
}
