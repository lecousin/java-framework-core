package net.lecousin.framework.core.tests.log;

import java.util.Calendar;

import net.lecousin.framework.application.LCCore;
import net.lecousin.framework.core.test.LCCoreAbstractTest;
import net.lecousin.framework.log.LogPattern;
import net.lecousin.framework.log.LogPattern.Log;
import net.lecousin.framework.log.Logger;

import org.junit.Assert;
import org.junit.Test;

public class TestLogPattern extends LCCoreAbstractTest {

	@Test
	public void tests() {
		Log log = new Log();
		log.level = Logger.Level.INFO;
		log.location = new Exception().getStackTrace()[0];
		log.loggerName = "test_logger";
		log.message = "this is the message";
		log.threadName = "theThread";
		log.timestamp = System.currentTimeMillis();
		log.app = LCCore.getApplication();
		
		expect("no pattern", log, "no pattern");
		expect("no pattern%", log, "no pattern%");
		expect("no pattern%%", log, "no pattern%");
		expect("test %- hello", log, "test %- hello");
		expect("test %d hello", log, "test %d hello");
		expect("test %d{ hello", log, "test %d{ hello");
		expect("test %d{yyyy} hello", log, "test " + Calendar.getInstance().get(Calendar.YEAR) + " hello");
		expect("file %f!", log, "file TestLogPattern.java!");
		expect("thread %t", log, "thread theThread");
		expect("test %l", log, "test %l");
		expect("test %le", log, "test %le");
		expect("test %lev", log, "test %lev");
		expect("test %leve", log, "test %leve");
		expect("test %levex", log, "test %levex");
		expect("test %levxx", log, "test %levxx");
		expect("test %lexxx", log, "test %lexxx");
		expect("test %lxxxx", log, "test %lxxxx");
		expect("test %level", log, "test INFO ");
		expect("test %lo", log, "test %lo");
		expect("test %log", log, "test %log");
		expect("test %logg", log, "test %logg");
		expect("test %logge", log, "test %logge");
		expect("test %logger", log, "test test_logger");
		expect("test %loggex", log, "test %loggex");
		expect("test %loggxx", log, "test %loggxx");
		expect("test %logxxx", log, "test %logxxx");
		expect("test %loxxxx", log, "test %loxxxx");
		expect("test %lxxxxx", log, "test %lxxxxx");
		expect("test %logger{", log, "test test_logger{");
		expect("test %loggerx", log, "test test_loggerx");
		expect("test %logger{}", log, "test test_logger");
		expect("test %logger{x}", log, "test test_logger");
		expect("test %logger{5}", log, "test ..ger");
		expect("test %a", log, "test %a");
		expect("test %ap", log, "test %ap");
		expect("test %app", log, "test %app");
		expect("test %appl", log, "test %appl");
		expect("test %appli", log, "test %appli");
		expect("test %applic", log, "test %applic");
		expect("test %applica", log, "test %applica");
		expect("test %applicat", log, "test %applicat");
		expect("test %applicati", log, "test %applicati");
		expect("test %applicatio", log, "test %applicatio");
		expect("test %application", log, "test " + log.app.getFullName());
		expect("test %applicatiox", log, "test %applicatiox");
		expect("test %application{", log, "test " + log.app.getFullName() + "{");
		expect("test %application{}", log, "test " + log.app.getFullName());
		expect("test %application{x}", log, "test " + log.app.getFullName());
		expect("test %application{5}", log, "test .." + log.app.getFullName().substring(log.app.getFullName().length() - 3, log.app.getFullName().length()));
		expect("test %ar", log, "test %ar");
		expect("test %art", log, "test %art");
		expect("test %arti", log, "test %arti");
		expect("test %artif", log, "test %artif");
		expect("test %artifa", log, "test %artifa");
		expect("test %artifac", log, "test %artifac");
		expect("test %artifact", log, "test %artifact");
		expect("test %artifactI", log, "test %artifactI");
		expect("test %artifactId", log, "test " + log.app.getArtifactId());
		expect("test %artifactIx", log, "test %artifactIx");
		expect("test %artifactId{", log, "test " + log.app.getArtifactId() + "{");
		expect("test %artifactId{}", log, "test " + log.app.getArtifactId());
		expect("test %artifactId{x}", log, "test " + log.app.getArtifactId());
		expect("test %artifactId{3}", log, "test .." + log.app.getArtifactId().substring(log.app.getArtifactId().length() - 1, log.app.getArtifactId().length()));
		expect("test %ax", log, "test %ax");
		expect("test: %m!", log, "test: this is the message!");
		expect("class %C---", log, "class net.lecousin.framework.core.tests.log.TestLogPattern---");
		expect("line %L,00", log, "line 20,00");
		expect("method %M", log, "method tests");
		
	}
	
	private static void expect(String pattern, Log log, String expected) {
		StringBuilder s = new LogPattern(pattern).generate(log);
		Assert.assertEquals(expected, s.toString());
	}
	
}
