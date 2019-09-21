package net.lecousin.framework.util;

import java.util.Locale;

/**
 * Get information about the system such as type of operating system, based on environment properties.
 */
public final class SystemEnvironment {
	
	public static final String SYSTEM_PROPERTY_USER_HOME = "user.home";
	
	private SystemEnvironment() { /* no instance */ }

	/** Type of operating system. */
	public enum OSFamily {
		dos("dos"),
		mac("mac"),
		netware("netware"),
		os2("os/2"),
		tandem("tandem"),
		unix("unix"),
		windows("windows"),
		win9x("win9x"),
		zos("z/os"),
		os400("os/400"),
		openvms("openvms");
		
		OSFamily(String name) {
			this.name = name;
		}
		
		private String name;

		public String getName() { return name; }
	}
	
	/** Returns the type of operating system. */
	public static OSFamily getOSFamily() {
		String name = System.getProperty("os.name").toLowerCase(Locale.US);
		if (name.contains("windows")) {
			if (name.contains("95") ||
				name.contains("98") ||
				name.contains("me") ||
				name.contains("ce"))
				return OSFamily.win9x;
			return OSFamily.windows;
		}
		if (name.contains("os/2"))
			return OSFamily.os2;
		if (name.contains("netware"))
			return OSFamily.netware;
		String sep = System.getProperty("path.separator");
		if (sep.equals(";"))
			return OSFamily.dos;
		if (name.contains("nonstop_kernel"))
			return OSFamily.tandem;
		if (name.contains("openvms"))
			return OSFamily.openvms;
		if (sep.equals(":") && (!name.contains("mac") || name.endsWith("x")))
			return OSFamily.unix;
		if (name.contains("mac"))
			return OSFamily.mac;
		if (name.contains("z/os") || name.contains("os/390"))
			return OSFamily.zos;
		if (name.contains("os/400"))
			return OSFamily.os400;
		return null;
	}
	
}
