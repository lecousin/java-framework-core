package net.lecousin.framework.core.tests.xml;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilderFactory;

import net.lecousin.framework.concurrent.Task;
import net.lecousin.framework.concurrent.Threading;
import net.lecousin.framework.io.IO;
import net.lecousin.framework.io.IOFromInputStream;
import net.lecousin.framework.xml.XMLStreamReader;
import net.lecousin.framework.xml.XMLStreamReaderAsync;
import net.lecousin.framework.xml.dom.XMLDocument;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

@RunWith(Parameterized.class)
public class TestDOMParsing extends TestDOM {

	private static final String[] files = {
		"xml-test-suite/mine/001.xml",
		"xml-test-suite/xmltest/valid/sa/001.xml",
		"xml-test-suite/xmltest/valid/sa/002.xml",
		"xml-test-suite/xmltest/valid/sa/003.xml",
		"xml-test-suite/xmltest/valid/sa/004.xml",
		"xml-test-suite/xmltest/valid/sa/005.xml",
		"xml-test-suite/xmltest/valid/sa/006.xml",
		"xml-test-suite/xmltest/valid/sa/007.xml",
		"xml-test-suite/xmltest/valid/sa/008.xml",
		"xml-test-suite/xmltest/valid/sa/009.xml",
		"xml-test-suite/xmltest/valid/sa/010.xml",
		"xml-test-suite/xmltest/valid/sa/011.xml",
		// name with a semi-colon is not allowed by latest version of XML "xml-test-suite/xmltest/valid/sa/012.xml",
		"xml-test-suite/xmltest/valid/sa/013.xml",
		"xml-test-suite/xmltest/valid/sa/014.xml",
		"xml-test-suite/xmltest/valid/sa/015.xml",
		"xml-test-suite/xmltest/valid/sa/016.xml",
		"xml-test-suite/xmltest/valid/sa/017.xml",
		"xml-test-suite/xmltest/valid/sa/018.xml",
		"xml-test-suite/xmltest/valid/sa/019.xml",
		"xml-test-suite/xmltest/valid/sa/020.xml",
		"xml-test-suite/xmltest/valid/sa/021.xml",
		"xml-test-suite/xmltest/valid/sa/022.xml",
		//"xml-test-suite/xmltest/valid/sa/023.xml",
		//"xml-test-suite/xmltest/valid/sa/024.xml",
		"xml-test-suite/xmltest/valid/sa/025.xml",
		"xml-test-suite/xmltest/valid/sa/026.xml",
		"xml-test-suite/xmltest/valid/sa/027.xml",
		"xml-test-suite/xmltest/valid/sa/028.xml",
		"xml-test-suite/xmltest/valid/sa/029.xml",
		"xml-test-suite/xmltest/valid/sa/030.xml",
		"xml-test-suite/xmltest/valid/sa/031.xml",
		"xml-test-suite/xmltest/valid/sa/032.xml",
		"xml-test-suite/xmltest/valid/sa/033.xml",
		"xml-test-suite/xmltest/valid/sa/034.xml",
		"xml-test-suite/xmltest/valid/sa/035.xml",
		"xml-test-suite/xmltest/valid/sa/036.xml",
		"xml-test-suite/xmltest/valid/sa/037.xml",
		"xml-test-suite/xmltest/valid/sa/038.xml",
		"xml-test-suite/xmltest/valid/sa/039.xml",
		"xml-test-suite/xmltest/valid/sa/040.xml",
		"xml-test-suite/xmltest/valid/sa/041.xml",
		"xml-test-suite/xmltest/valid/sa/042.xml",
		"xml-test-suite/xmltest/valid/sa/043.xml",
		//"xml-test-suite/xmltest/valid/sa/044.xml",
		//"xml-test-suite/xmltest/valid/sa/045.xml",
		//"xml-test-suite/xmltest/valid/sa/046.xml",
		"xml-test-suite/xmltest/valid/sa/047.xml",
		"xml-test-suite/xmltest/valid/sa/048.xml",
		"xml-test-suite/xmltest/valid/sa/049.xml",
		"xml-test-suite/xmltest/valid/sa/050.xml",
		"xml-test-suite/xmltest/valid/sa/051.xml",
		"xml-test-suite/xmltest/valid/sa/052.xml",
		//"xml-test-suite/xmltest/valid/sa/053.xml",
		"xml-test-suite/xmltest/valid/sa/054.xml",
		"xml-test-suite/xmltest/valid/sa/055.xml",
		"xml-test-suite/xmltest/valid/sa/056.xml",
		"xml-test-suite/xmltest/valid/sa/057.xml",
		//"xml-test-suite/xmltest/valid/sa/058.xml",
		"xml-test-suite/xmltest/valid/sa/059.xml",
		"xml-test-suite/xmltest/valid/sa/060.xml",
		"xml-test-suite/xmltest/valid/sa/061.xml",
		"xml-test-suite/xmltest/valid/sa/062.xml",
		"xml-test-suite/xmltest/valid/sa/063.xml",
		"xml-test-suite/xmltest/valid/sa/064.xml",
		"xml-test-suite/xmltest/valid/sa/065.xml",
		//"xml-test-suite/xmltest/valid/sa/066.xml",
		"xml-test-suite/xmltest/valid/sa/067.xml",
		//"xml-test-suite/xmltest/valid/sa/068.xml",
		"xml-test-suite/xmltest/valid/sa/069.xml",
		"xml-test-suite/xmltest/valid/sa/070.xml",
		"xml-test-suite/xmltest/valid/sa/071.xml",
		"xml-test-suite/xmltest/valid/sa/072.xml",
		"xml-test-suite/xmltest/valid/sa/073.xml",
		"xml-test-suite/xmltest/valid/sa/074.xml",
		"xml-test-suite/xmltest/valid/sa/075.xml",
		"xml-test-suite/xmltest/valid/sa/076.xml",
		"xml-test-suite/xmltest/valid/sa/077.xml",
		"xml-test-suite/xmltest/valid/sa/078.xml",
		"xml-test-suite/xmltest/valid/sa/079.xml",
		//"xml-test-suite/xmltest/valid/sa/080.xml",
		"xml-test-suite/xmltest/valid/sa/081.xml",
		"xml-test-suite/xmltest/valid/sa/082.xml",
		"xml-test-suite/xmltest/valid/sa/083.xml",
		"xml-test-suite/xmltest/valid/sa/084.xml",
		//"xml-test-suite/xmltest/valid/sa/085.xml",
		//"xml-test-suite/xmltest/valid/sa/086.xml",
		//"xml-test-suite/xmltest/valid/sa/087.xml",
		//"xml-test-suite/xmltest/valid/sa/088.xml",
		//"xml-test-suite/xmltest/valid/sa/089.xml",
		"xml-test-suite/xmltest/valid/sa/090.xml",
		//"xml-test-suite/xmltest/valid/sa/091.xml",
		"xml-test-suite/xmltest/valid/sa/092.xml",
		"xml-test-suite/xmltest/valid/sa/093.xml",
		//"xml-test-suite/xmltest/valid/sa/094.xml",
		"xml-test-suite/xmltest/valid/sa/095.xml",
		//"xml-test-suite/xmltest/valid/sa/096.xml",
		//"xml-test-suite/xmltest/valid/sa/097.xml",
		"xml-test-suite/xmltest/valid/sa/098.xml",
		"xml-test-suite/xmltest/valid/sa/099.xml",
		"xml-test-suite/xmltest/valid/sa/100.xml",
		"xml-test-suite/xmltest/valid/sa/101.xml",
		"xml-test-suite/xmltest/valid/sa/102.xml",
		"xml-test-suite/xmltest/valid/sa/103.xml",
		//"xml-test-suite/xmltest/valid/sa/104.xml",
		"xml-test-suite/xmltest/valid/sa/105.xml",
		"xml-test-suite/xmltest/valid/sa/106.xml",
		"xml-test-suite/xmltest/valid/sa/107.xml",
		// entity ref "xml-test-suite/xmltest/valid/sa/108.xml",
		"xml-test-suite/xmltest/valid/sa/109.xml",
		// entity ref "xml-test-suite/xmltest/valid/sa/110.xml",
		//"xml-test-suite/xmltest/valid/sa/111.xml",
		"xml-test-suite/xmltest/valid/sa/112.xml",
		"xml-test-suite/xmltest/valid/sa/113.xml",
		//"xml-test-suite/xmltest/valid/sa/114.xml",
		//"xml-test-suite/xmltest/valid/sa/115.xml",
		//"xml-test-suite/xmltest/valid/sa/116.xml",
		//"xml-test-suite/xmltest/valid/sa/117.xml",
		//"xml-test-suite/xmltest/valid/sa/118.xml",
		"xml-test-suite/xmltest/valid/sa/119.xml",
	};
		
	@Parameters(name = "file = {0}")
	public static Collection<Object[]> parameters() {
		ArrayList<Object[]> list = new ArrayList<>(files.length);
		for (int i = 0; i < files.length; ++i)
			list.add(new Object[] { files[i] });
		return list;
	}
	
	public TestDOMParsing(String filepath) {
		this.filepath = filepath;
	}
	
	private String filepath;
	
	@Test(timeout=120000)
	public void testReaderSync() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		InputStream in = getClass().getClassLoader().getResourceAsStream(filepath);
		Document doc = factory.newDocumentBuilder().parse(new InputSource(in));
		InputStream in2 = getClass().getClassLoader().getResourceAsStream(filepath);
		IO.Readable io = new IOFromInputStream(in2, filepath, Threading.getDrivesTaskManager().getTaskManager(new File(".")), Task.PRIORITY_NORMAL);
		XMLStreamReader xml = new XMLStreamReader(io, 1024);
		xml.start();
		XMLDocument doc2 = XMLDocument.create(xml);
		checkDocument(doc, doc2);
		in2.close();
		in.close();
	}
	
	@Test(timeout=120000)
	public void testReaderAsync() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(true);
		InputStream in = getClass().getClassLoader().getResourceAsStream(filepath);
		Document doc = factory.newDocumentBuilder().parse(new InputSource(in));
		InputStream in2 = getClass().getClassLoader().getResourceAsStream(filepath);
		IO.Readable io = new IOFromInputStream(in2, filepath, Threading.getDrivesTaskManager().getTaskManager(new File(".")), Task.PRIORITY_NORMAL);
		XMLStreamReaderAsync xml = new XMLStreamReaderAsync(io, 1024);
		xml.start().blockException(0);
		XMLDocument doc2 = XMLDocument.create(xml).blockResult(0);
		checkDocument(doc, doc2);
		in2.close();
		in.close();
	}
	
}
