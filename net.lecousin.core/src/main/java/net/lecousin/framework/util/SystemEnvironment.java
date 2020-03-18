package net.lecousin.framework.util;

import java.util.Locale;

/**
 * Get information about the system such as type of operating system, based on environment properties.
 */
public final class SystemEnvironment {
	
	public static final String SYSTEM_PROPERTY_USER_HOME = "user.home";
	
	private SystemEnvironment() {
		/* no instance */
	}

	/** Type of operating system. */
	public enum OSFamily {
		DOS("dos"),
		MAC("mac"),
		NETWARE("netware"),
		OS2("os/2"),
		TANDEM("tandem"),
		UNIX("unix"),
		WINDOWS("windows"),
		WIN9X("win9x"),
		ZOS("z/os"),
		OS400("os/400"),
		OPENVMS("openvms");
		
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
				return OSFamily.WIN9X;
			return OSFamily.WINDOWS;
		}
		if (name.contains("os/2"))
			return OSFamily.OS2;
		if (name.contains("netware"))
			return OSFamily.NETWARE;
		String sep = System.getProperty("path.separator");
		if (sep.equals(";"))
			return OSFamily.DOS;
		if (name.contains("nonstop_kernel"))
			return OSFamily.TANDEM;
		if (name.contains("openvms"))
			return OSFamily.OPENVMS;
		if (sep.equals(":") && (!name.contains("mac") || name.endsWith("x")))
			return OSFamily.UNIX;
		if (name.contains("mac"))
			return OSFamily.MAC;
		if (name.contains("z/os") || name.contains("os/390"))
			return OSFamily.ZOS;
		if (name.contains("os/400"))
			return OSFamily.OS400;
		return null;
	}
	
}
