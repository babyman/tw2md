package ca.codepit.tw2md;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author evan
 */
class DateToolsTest {

	@Test
	public void canParseATiddlyWikiTimestamp() {
		// Timestamp is always UTC, see https://en.wikipedia.org/wiki/Unix_time
		//
		// We explicitely convert first to ZonedDateTime UTC to avoid broken test 
		// thanks to timezone differences between devs computers around the world.
		final String tiddlyWikiTimestampInUTC = "20210920015946441";
		final ZonedDateTime expectedUTCZonedDateTime = ZonedDateTime.of(LocalDateTime.of(2021, 9, 20, 01, 59, 46, 441 * 1000000), ZoneId.of("UTC"));


		final LocalDateTime expectedLocalDateTimeOnSystemTimeZone = expectedUTCZonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
		final Optional<LocalDateTime> parsedTiddlyWikiTimestampAsLocalDateTimeOnSystemTimeZone = DateTools.parseTiddlyWikiTimestampAsLocalDateTime(tiddlyWikiTimestampInUTC);
		assertEquals(Optional.of(expectedLocalDateTimeOnSystemTimeZone), parsedTiddlyWikiTimestampAsLocalDateTimeOnSystemTimeZone);
	}

}
