/*
 * Copyright (C) 2025, Moderne Inc. and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0 which is available at
 * https://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */
package org.openrewrite.jgit.errors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openrewrite.jgit.transport.URIish;

/**
 * Exception thrown when an HTTP transport operation fails with an error status.
 * Provides access to HTTP status code, response headers, and response body
 * for logging and analysis by consumers.
 */
public class HttpResponseException extends TransportException {
	private static final long serialVersionUID = 1L;

	private final int statusCode;
	private final Map<String, List<String>> headers;
	private final String responseBody;

	/**
	 * Constructs an HttpResponseException with full HTTP response details.
	 *
	 * @param uri
	 *            URI used for transport
	 * @param statusCode
	 *            HTTP status code (e.g., 403, 429, 500)
	 * @param headers
	 *            response headers, may be null
	 * @param responseBody
	 *            response body content, may be null
	 * @param message
	 *            error message
	 */
	public HttpResponseException(URIish uri, int statusCode,
			Map<String, List<String>> headers, String responseBody,
			String message) {
		super(uri, message);
		this.statusCode = statusCode;
		this.headers = headers != null ? headers : Collections.emptyMap();
		this.responseBody = responseBody;
	}

	/**
	 * Get the HTTP status code.
	 *
	 * @return HTTP status code (e.g., 403, 429, 500)
	 */
	public int getStatusCode() {
		return statusCode;
	}

	/**
	 * Get all response headers.
	 *
	 * @return unmodifiable map of response headers
	 */
	public Map<String, List<String>> getHeaders() {
		return Collections.unmodifiableMap(headers);
	}

	/**
	 * Get a specific header value. Header name lookup is case-insensitive.
	 * If the header has multiple values, returns the first one.
	 *
	 * @param name
	 *            header name
	 * @return header value, or null if not present
	 */
	public String getHeader(String name) {
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			if (name.equalsIgnoreCase(entry.getKey())) {
				List<String> values = entry.getValue();
				return (values != null && !values.isEmpty()) ? values.get(0) : null;
			}
		}
		return null;
	}

	/**
	 * Get the response body content.
	 *
	 * @return response body, or null if not available
	 */
	public String getResponseBody() {
		return responseBody;
	}
}
