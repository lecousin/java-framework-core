package net.lecousin.framework.core.tests.util;

import net.lecousin.framework.application.launcher.Launcher;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.util.CommandLine;
import net.lecousin.framework.util.CommandLine.ArgumentsConsumer;

import org.junit.Assert;
import org.junit.Test;

public class TestCommandLine extends LCCoreAbstractTest {

	@Test(timeout=15000)
	public void testGetOptionValue() {
		String[] args = new String[] { "hello", "-test", "world", "-aa=bb", "-hello=world", "cc=dd" };
		Assert.assertNull(CommandLine.getOptionValue(args, "toto"));
		Assert.assertNull(CommandLine.getOptionValue(args, "test"));
		Assert.assertNull(CommandLine.getOptionValue(args, "world"));
		Assert.assertNull(CommandLine.getOptionValue(args, "cc"));
		Assert.assertEquals("bb", CommandLine.getOptionValue(args, "aa"));
		Assert.assertEquals("world", CommandLine.getOptionValue(args, "hello"));
	}

	@SuppressWarnings("unchecked")
	@Test(timeout=15000)
	public void testParse() throws Exception {
		Launcher.CommandLineContext context = new Launcher.CommandLineContext();
		String[] args = new String[] { "-artifactId=myArtifact", "-groupId=myGroup", "-parameters", "p1", "p2" };
		CommandLine.parse(args, context, new ArgumentsConsumer[][] {
			new ArgumentsConsumer[] {
				new Launcher.CommandLineContext.GroupIdConsumer(),
				new Launcher.CommandLineContext.ArtifactIdConsumer(),
			}, new ArgumentsConsumer[] {
				new Launcher.CommandLineContext.AppParametersConsumer()
			}
		});
		Assert.assertEquals("myGroup", context.groupId);
		Assert.assertEquals("myArtifact", context.artifactId);
		Assert.assertNull(context.version);
		Assert.assertEquals(2, context.appParameters.length);
		Assert.assertEquals("p1", context.appParameters[0]);
		Assert.assertEquals("p2", context.appParameters[1]);
		
		try {
			args = new String[] { "-artifactId=myArtifact", "-groupId=myGroup", "X" };
			context = new Launcher.CommandLineContext();
			CommandLine.parse(args, context, new ArgumentsConsumer[][] {
				new ArgumentsConsumer[] {
					new Launcher.CommandLineContext.GroupIdConsumer(),
					new Launcher.CommandLineContext.ArtifactIdConsumer(),
				}, new ArgumentsConsumer[] {
					new Launcher.CommandLineContext.AppParametersConsumer()
				}
			});
			throw new AssertionError();
		} catch (IllegalArgumentException e) {
			// ok
		}
	}

}
