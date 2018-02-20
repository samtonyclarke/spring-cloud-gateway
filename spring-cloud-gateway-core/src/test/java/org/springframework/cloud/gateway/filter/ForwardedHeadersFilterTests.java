/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.springframework.cloud.gateway.filter;

import org.junit.Test;

import org.springframework.cloud.gateway.filter.ForwardedHeadersFilter.Forwarded;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.gateway.filter.ForwardedHeadersFilter.FORWARDED_HEADER;
import static org.springframework.cloud.gateway.filter.ForwardedHeadersFilter.parse;

/**
 * @author Spencer Gibb
 */
public class ForwardedHeadersFilterTests {

	@Test
	public void forwardedHeaderDoesNotExist() {
		MockServerHttpRequest request = MockServerHttpRequest
				.get("http://localhost/get")
				.header(HttpHeaders.HOST, "myhost")
				.build();

		ForwardedHeadersFilter filter = new ForwardedHeadersFilter();

		HttpHeaders headers = filter.filter(request.getHeaders());

		assertThat(headers.get(FORWARDED_HEADER)).hasSize(1);

		String forwarded = headers.getFirst(FORWARDED_HEADER);

		assertThat(forwarded).isEqualTo("host=myhost");
	}

	@Test
	public void forwardedParsedCorrectly() {
		String[] valid = new String[] {
				"for=\"_gazonk\"",
				"for=192.0.2.60;proto=http;by=203.0.113.43",
				"for=192.0.2.43, for=198.51.100.17",
				"for=12.34.56.78;host=example.com;proto=https, for=23.45.67.89",
				"for=12.34.56.78, for=23.45.67.89;secret=egah2CGj55fSJFs, for=10.1.2.3",
				"For=\"[2001:db8:cafe::17]:4711\"",
		};

		List<Map<String, String>> expectedFor = new ArrayList<Map<String, String>>() {{
				add(map("for", "\"_gazonk\""));
				add(map("for", "192.0.2.60"));
				add(map("for", "192.0.2.43,198.51.100.17"));
				add(map("for", "12.34.56.78,23.45.67.89"));
				add(map("for", "12.34.56.78,23.45.67.89,10.1.2.3"));
				add(map("for", "\"[2001:db8:cafe::17]:4711\""));
		}};

		for (int i = 0; i < valid.length; i++) {
			String value = valid[i];
			// simulate spring's parsed headers
			String[] values = StringUtils.tokenizeToStringArray(value, ",");
			List<Forwarded> results = parse(Arrays.asList(values));
			System.out.println(results);

			assertThat(results).hasSize(values.length);
			assertThat(results.get(0)).isNotNull();

			Map<String, String> expected = expectedFor.get(i);
			System.out.println(Arrays.asList(expected));
			for (int j = 0; j < results.size(); j++) {
				Forwarded forwarded = results.get(j);
				assertThat(forwarded.getValues()).containsEntry("for", expected.get("for"));
			}
		}
	}

	private Map<String, String> map(String... values) {
		if (values.length % 2 != 0) {
			throw new IllegalArgumentException("values must have even number of items: "+ Arrays.asList(values));
		}
		HashMap<String, String> map = new HashMap<>();
		for (int i = 0; i < values.length; i++) {
			map.put(values[i], values[++i]);
		}
		return map;
	}

}
