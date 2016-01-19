package org.eclipse.californium.core.test;

import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class MediaTypeRegistryTest {

	@Test
	public void testGetAllMediaTypes() throws Exception {
		Set<Integer> all = MediaTypeRegistry.getAllMediaTypes();

		assertTrue(all.contains(MediaTypeRegistry.TEXT_PLAIN));
		assertTrue(all.contains(MediaTypeRegistry.APPLICATION_LINK_FORMAT));
		assertTrue(all.contains(MediaTypeRegistry.APPLICATION_XML));
		assertTrue(all.contains(MediaTypeRegistry.APPLICATION_OCTET_STREAM));
		assertTrue(all.contains(MediaTypeRegistry.APPLICATION_EXI));
		assertTrue(all.contains(MediaTypeRegistry.APPLICATION_JSON));
		assertTrue(all.contains(MediaTypeRegistry.APPLICATION_CBOR));
	}

	@Test
	public void testIsPrintable() throws Exception {
		assertTrue(MediaTypeRegistry.isPrintable(MediaTypeRegistry.TEXT_PLAIN));
		assertTrue(MediaTypeRegistry.isPrintable(MediaTypeRegistry.APPLICATION_LINK_FORMAT));
		assertTrue(MediaTypeRegistry.isPrintable(MediaTypeRegistry.APPLICATION_XML));
		assertFalse(MediaTypeRegistry.isPrintable(MediaTypeRegistry.APPLICATION_OCTET_STREAM));
		assertFalse(MediaTypeRegistry.isPrintable(MediaTypeRegistry.APPLICATION_EXI));
		assertTrue(MediaTypeRegistry.isPrintable(MediaTypeRegistry.APPLICATION_JSON));
		assertFalse(MediaTypeRegistry.isPrintable(MediaTypeRegistry.APPLICATION_CBOR));
	}

	@Test
	public void testParse() throws Exception {
		assertEquals(MediaTypeRegistry.TEXT_PLAIN, MediaTypeRegistry.parse("text/plain"));
		assertEquals(MediaTypeRegistry.APPLICATION_LINK_FORMAT, MediaTypeRegistry.parse("application/link-format"));
		assertEquals(MediaTypeRegistry.APPLICATION_XML, MediaTypeRegistry.parse("application/xml"));
		assertEquals(MediaTypeRegistry.APPLICATION_OCTET_STREAM, MediaTypeRegistry.parse("application/octet-stream"));
		assertEquals(MediaTypeRegistry.APPLICATION_EXI, MediaTypeRegistry.parse("application/exi"));
		assertEquals(MediaTypeRegistry.APPLICATION_JSON, MediaTypeRegistry.parse("application/json"));
		assertEquals(MediaTypeRegistry.APPLICATION_CBOR, MediaTypeRegistry.parse("application/cbor"));

		assertEquals(MediaTypeRegistry.UNDEFINED, MediaTypeRegistry.parse("foobar"));
		assertEquals(MediaTypeRegistry.UNDEFINED, MediaTypeRegistry.parse(null));
	}

	@Test
	public void testParseWildcard() throws Exception {
		Set<Integer> expectedText = new HashSet<>(Arrays.asList(
				MediaTypeRegistry.TEXT_PLAIN
		));

		Set<Integer> expectedApplication = new HashSet<>(Arrays.asList(
				MediaTypeRegistry.APPLICATION_LINK_FORMAT,
				MediaTypeRegistry.APPLICATION_XML,
				MediaTypeRegistry.APPLICATION_OCTET_STREAM,
				MediaTypeRegistry.APPLICATION_EXI,
				MediaTypeRegistry.APPLICATION_JSON,
				MediaTypeRegistry.APPLICATION_CBOR
		));

		assertTrue(new HashSet<>(Arrays.asList(MediaTypeRegistry.parseWildcard("text*"))).containsAll(expectedText));
		assertTrue(new HashSet<>(Arrays.asList(MediaTypeRegistry.parseWildcard("application*"))).containsAll(expectedApplication));
		assertFalse(new HashSet<>(Arrays.asList(MediaTypeRegistry.parseWildcard("application*"))).containsAll(expectedText));
	}

	@Test
	public void testToFileExtension() throws Exception {
		assertEquals("txt", MediaTypeRegistry.toFileExtension(MediaTypeRegistry.TEXT_PLAIN));
		assertEquals("wlnk", MediaTypeRegistry.toFileExtension(MediaTypeRegistry.APPLICATION_LINK_FORMAT));
		assertEquals("xml", MediaTypeRegistry.toFileExtension(MediaTypeRegistry.APPLICATION_XML));
		assertEquals("bin", MediaTypeRegistry.toFileExtension(MediaTypeRegistry.APPLICATION_OCTET_STREAM));
		assertEquals("exi", MediaTypeRegistry.toFileExtension(MediaTypeRegistry.APPLICATION_EXI));
		assertEquals("json", MediaTypeRegistry.toFileExtension(MediaTypeRegistry.APPLICATION_JSON));
		assertEquals("cbor", MediaTypeRegistry.toFileExtension(MediaTypeRegistry.APPLICATION_CBOR));
	}

	@Test
	public void testToString() throws Exception {
		assertEquals("text/plain", MediaTypeRegistry.toString(MediaTypeRegistry.TEXT_PLAIN));
		assertEquals("application/link-format", MediaTypeRegistry.toString(MediaTypeRegistry.APPLICATION_LINK_FORMAT));
		assertEquals("application/xml", MediaTypeRegistry.toString(MediaTypeRegistry.APPLICATION_XML));
		assertEquals("application/octet-stream", MediaTypeRegistry.toString(MediaTypeRegistry.APPLICATION_OCTET_STREAM));
		assertEquals("application/exi", MediaTypeRegistry.toString(MediaTypeRegistry.APPLICATION_EXI));
		assertEquals("application/json", MediaTypeRegistry.toString(MediaTypeRegistry.APPLICATION_JSON));
		assertEquals("application/cbor", MediaTypeRegistry.toString(MediaTypeRegistry.APPLICATION_CBOR));
	}

	@Test
	public void testAddAndGet() throws Exception {
		// test using the obsolete-draft PNG spec
		MediaTypeRegistry.MediaType png = new MediaTypeRegistry.MediaType(23, "image/png", "png", false);

		assertEquals(null, MediaTypeRegistry.get(png.code));

		MediaTypeRegistry.add(png);

		assertEquals(png, MediaTypeRegistry.get(png.code));
	}
}