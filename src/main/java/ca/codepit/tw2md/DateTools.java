package ca.codepit.tw2md;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.yaml.snakeyaml.scanner.Constant;

/**
 * @author evan
 */
public class DateTools {
	private static final String tiddlyWikiDateTimeFormat = "uuuuMMddHHmmssSSSXXXXX";

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
					.ofPattern(tiddlyWikiDateTimeFormat);

	public static Optional<LocalDateTime> parseTiddlyWikiTimestampAsLocalDateTime(String tiddlyWikiTimestampUTC) {

		return parseTiddlyWikiTimestampAsSystemZonedDateTime(tiddlyWikiTimestampUTC).map(ZonedDateTime::toLocalDateTime);
	}

	public static Optional<ZonedDateTime> parseTiddlyWikiTimestampAsSystemZonedDateTime(String tiddlyWikiTimestamp) {

		try {
			ZonedDateTime zonedDateTimeInstanceUTC = ZonedDateTime.parse(tiddlyWikiTimestamp + "+00:00", DATE_TIME_FORMATTER);

			ZonedDateTime zonedDateTimeInstanceLocalizedToComputer = zonedDateTimeInstanceUTC.withZoneSameInstant(ZoneOffset.systemDefault());

			return Optional.of(zonedDateTimeInstanceLocalizedToComputer);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
