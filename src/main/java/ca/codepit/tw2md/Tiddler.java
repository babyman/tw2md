package ca.codepit.tw2md;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author evan
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
class Tiddler {

	private final Map<String, String> headers;

	private final List<String> body;

	private final Optional<ZonedDateTime> createdTime;

	private final Optional<ZonedDateTime> lastUpdatedTime;

	public Tiddler(Map<String, String> headers,
								 List<String> body) {

		this.headers = headers;
		this.body = body;

		this.createdTime = Optional.ofNullable(headers.get("created"))
						.flatMap(DateTools::parseTiddlyWikiTimestampAsSystemZonedDateTime);

		this.lastUpdatedTime = Optional.ofNullable(headers.get("modified"))
						.flatMap(DateTools::parseTiddlyWikiTimestampAsSystemZonedDateTime);
	}

	public String getHeader(String key) {

		return headers.get(key);
	}

	public Map<String, String> getHeaders() {

		return headers;
	}

	public List<String> getBody() {

		return body;
	}

	public Optional<ZonedDateTime> getCreatedTime() {

		return createdTime;
	}

	public Optional<ZonedDateTime> getLastUpdatedTime() {

		return lastUpdatedTime;
	}

	@Override
	public String toString() {

		return "Tiddler{" +
						"header=" + headers +
						", body=" + body +
						'}';
	}
}
