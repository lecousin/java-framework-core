package net.lecousin.core.javaee;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import net.lecousin.framework.application.Application;
import net.lecousin.framework.application.Artifact;
import net.lecousin.framework.application.Version;
import net.lecousin.framework.application.libraries.classpath.DefaultLibrariesManager;
import net.lecousin.framework.util.PropertiesUtil;

public class BootstrapServlet extends GenericServlet {

	private static final long serialVersionUID = -6358069042161072659L;

	@Override
	@SuppressWarnings({
		"squid:S2095", // input is closed
		"squid:S106", // use System.err
		"squid:S3776", // complexity
	})
	public void init() throws ServletException {
		Map<String, String> properties = null;
		String propertiesURL = getServletConfig().getInitParameter("properties");
		if (propertiesURL != null && !propertiesURL.isEmpty()) {
			String[] urls = propertiesURL.split(",");
			for (String u : urls) {
				if (u.isEmpty()) continue;
				try {
					InputStream input;
					if (u.toLowerCase().startsWith("classpath:")) {
						input = getClass().getClassLoader().getResourceAsStream(u.substring(10));
						if (input == null) throw new FileNotFoundException("File not found: " + u.substring(10));
					} else {
						URL url = new URL(u);
						input = url.openStream();
					}
					Properties props = new Properties();
					try {
						props.load(input);
					} finally {
						input.close();
					}
					properties = new HashMap<>();
					for (Map.Entry<Object, Object> p : props.entrySet())
						properties.put(p.getKey().toString(), p.getValue().toString());
					PropertiesUtil.resolve(properties);
				} catch (Exception t) {
					throw new ServletException("Error loading properties from " + u, t);
				}
			}
		}
		
		InitialContext ctx;
		try { ctx = new InitialContext(); }
		catch (Exception e) {
			throw new ServletException("Unable to get InitialContext for JNDI lookup", e);
		}
		
		String threadFactoryName = getServletConfig().getInitParameter("threadFactory");
		ThreadFactory threadFactory;
		try {
			threadFactory = (ManagedThreadFactory)ctx.lookup("java:comp/" + (threadFactoryName != null ? threadFactoryName : "DefaultManagedThreadFactory"));
		} catch (Exception e) {
			if (threadFactoryName != null)
				throw new ServletException("Unable to lookup for ManagedThreadFactory", e);
			System.err.println("No DefaultManagedThreadFactory found");
			threadFactory = Executors.defaultThreadFactory();
		}

		
		String groupId = get("groupId", properties);
		if (groupId == null) throw new ServletException("Missing groupId init-param");
		String artifactId = get("artifactId", properties);
		if (artifactId == null) throw new ServletException("Missing artifactId init-param");
		String version = get("version", properties);
		if (version == null) throw new ServletException("Missing version init-param");
		
		String s = get("debug", properties);
		boolean debugMode = !"false".equals(s);
		
		Application.start(
			new Artifact(groupId, artifactId, new Version(version)),
			new String[0],
			properties,
			debugMode,
			threadFactory,
			new DefaultLibrariesManager(),
			null
		);
	}
	
	private String get(String name, Map<String, String> properties) {
		String value = getServletConfig().getInitParameter(name);
		if (value != null && value.isEmpty()) value = null;
		if (value == null && properties != null) value = properties.get(name);
		if (value != null && properties != null) value = PropertiesUtil.resolve(value, properties);
		return value;
	}
	
	@Override
	public void service(ServletRequest req, ServletResponse res) {
		// nothing
	}

}
