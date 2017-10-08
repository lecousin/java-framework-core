package net.lecousin.framework.util;

/** Utility methods for command line parameters. */
public final class CommandLineUtil {
	
	private CommandLineUtil() { /* no instance. */ }

	/**
	 * Search for an argument "-&lt;option&gt;=&lt;value&gt;" where option is the given option name, and return the value.
	 */
	public static String getOptionValue(String[] args, String optionName) {
		String opt = "-" + optionName + "=";
		for (String arg : args) {
			if (arg.startsWith(opt))
				return arg.substring(opt.length());
		}
		return null;
	}
	
}
