/*******************************************************************************
 * Copyright (c) 2015 Institute for Pervasive Computing, ETH Zurich and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *    Matthias Kovatsch - creator and main architect
 *    Martin Lanter - architect and re-implementation
 *    Dominique Im Obersteg - parsers and initial implementation
 *    Daniel Pauli - parsers and initial implementation
 *    Kai Hudalla - logging
 ******************************************************************************/
package org.eclipse.californium.core.coap;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * This class describes the CoAP Media Type Registry as defined in
 * RFC 7252, Section 12.3.
 */
public class MediaTypeRegistry {

	// Constants ///////////////////////////////////////////////////////////////
	// IANA registry at http://www.iana.org/assignments/core-parameters/core-parameters.xhtml#content-formats
	public static final int TEXT_PLAIN = 0;
	public static final int APPLICATION_LINK_FORMAT = 40;
	public static final int APPLICATION_XML = 41;
	public static final int APPLICATION_OCTET_STREAM = 42;
	public static final int APPLICATION_EXI = 47;
	public static final int APPLICATION_JSON = 50;
	public static final int APPLICATION_CBOR = 60;

	// implementation specific
	public static final int UNDEFINED = -1;

	// initializer
	private static final HashMap<Integer, MediaType> registry = new HashMap<>();
	static {
		add(new MediaType(UNDEFINED, "unknown", "???", true));

		add(new MediaType(TEXT_PLAIN, "text/plain", "txt", true));

		add(new MediaType(APPLICATION_LINK_FORMAT, "application/link-format", "wlnk", true));
		add(new MediaType(APPLICATION_XML, "application/xml", "xml", true));
		add(new MediaType(APPLICATION_OCTET_STREAM, "application/octet-stream", "bin", false));
		add(new MediaType(APPLICATION_EXI, "application/exi", "exi", false));
		add(new MediaType(APPLICATION_JSON, "application/json", "json", true));
		add(new MediaType(APPLICATION_CBOR, "application/cbor", "cbor", false)); // RFC 7049
	}

	// Static Functions ////////////////////////////////////////////////////////

	public static Set<Integer> getAllMediaTypes() {
		return registry.keySet();
	}

	public static boolean isPrintable(int mediaType) {
		MediaType type = registry.get(mediaType);

		if (type != null) {
			return type.printable;
		} else {
			return true;
		}
	}

	public static int parse(String type) {
		if (type == null) {
			return UNDEFINED;
		}

		for (Integer key : registry.keySet()) {
			if (registry.get(key).type.equalsIgnoreCase(type)) {
				return key;
			}
		}

		return UNDEFINED;
	}

	/**
	 * find all media types with a specifier matching a shell-style glob
	 * @param regex a shell-style glob that contains exactly one '*', ex: "application/*"
	 * @return an array of the matching media types
	 */
	public static Integer[] parseWildcard(String regex) {
		regex = regex.trim().substring(0, regex.indexOf('*')).trim().concat(".*");
		Pattern pattern = Pattern.compile(regex);
		List<Integer> matches = new LinkedList<Integer>();

		for (Integer mediaType : registry.keySet()) {
			String mime = registry.get(mediaType).type;
			if (pattern.matcher(mime).matches()) {
				matches.add(mediaType);
			}
		}

		return matches.toArray(new Integer[0]);
	}

	public static String toFileExtension(int mediaType) {
		MediaType type = registry.get(mediaType);

		if (type != null) {
			return type.fileExtension;
		} else {
			return "unknown_" + mediaType;
		}
	}

	public static String toString(int mediaType) {
		MediaType type = registry.get(mediaType);

		if (type != null) {
			return type.type;
		} else {
			return "unknown/" + mediaType;
		}
	}

	public static void add(MediaType m) {
		registry.put(m.code, m);
	}

	public static MediaType get(int code) {
		return registry.get(code);
	}

	public static class MediaType {
		// IANA-assigned CoAP content format ID
		public final int code;

		// IANA-assigned media type string
		public final String type;

		// file extension
		public final String fileExtension;

		// true if format is human-readable
		public final boolean printable;

		public MediaType(int code, String type, String fileExtension, boolean printable) {
			this.code = code;
			this.type = type;
			this.fileExtension = fileExtension;
			this.printable = printable;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			MediaType mediaType = (MediaType) o;

			if (code != mediaType.code) return false;
			if (printable != mediaType.printable) return false;
			if (!type.equals(mediaType.type)) return false;
			return fileExtension.equals(mediaType.fileExtension);

		}

		@Override
		public int hashCode() {
			int result = code;
			result = 31 * result + type.hashCode();
			result = 31 * result + fileExtension.hashCode();
			result = 31 * result + (printable ? 1 : 0);
			return result;
		}
	}
}
