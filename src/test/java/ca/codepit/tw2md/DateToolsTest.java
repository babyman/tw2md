package ca.codepit.tw2md;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author evan
 */
class DateToolsTest {

	@Test
	public void canParseATiddlyWikiTimestamp() {

		final LocalDateTime expected = LocalDateTime.of(2021, 9, 19, 21, 59, 46, 441 * 1000000);
		final Optional<LocalDateTime> ts = DateTools.ts("20210920015946441");
		assertEquals(Optional.of(expected), ts);
	}

}
