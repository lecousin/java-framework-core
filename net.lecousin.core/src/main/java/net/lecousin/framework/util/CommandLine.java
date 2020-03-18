package net.lecousin.framework.util;

/** Utility methods for command line parameters. */
public final class CommandLine {
	
	private CommandLine() {
		/* no instance. */
	}

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

	/** Command line arguments consumer.
	 * @param <T> type of context
	 */
	public static interface ArgumentsConsumer<T> {
		
		/** Return true if the given argument matches this consumer. */
		boolean matches(String arg, T context);
		
		/** Consume arguments starting at the given index, return the number of arguments consumed. */
		@SuppressWarnings("squid:S00112") // Exception
		int consume(String[] args, int index, T context) throws Exception;
		
	}
	
	/**
	 * Parse the command line arguments.
	 * @param <T> type of context
	 * @param args command line arguments
	 * @param context context
	 * @param steps consumers for each step
	 */
	public static <T> void parse(String[] args, T context, ArgumentsConsumer<T>[][] steps) throws Exception {
		int index = 0;
		int stepIndex = 0;
		while (index < args.length) {
			if (stepIndex == steps.length)
				throw new IllegalArgumentException("Unexpected argument: " + args[index]);
			boolean found = false;
			for (ArgumentsConsumer<T> consumer : steps[stepIndex]) {
				if (!consumer.matches(args[index], context))
					continue;
				found = true;
				int eaten = consumer.consume(args, index, context);
				index += eaten;
				break;
			}
			if (!found)
				stepIndex++;
		}
	}
	
}
