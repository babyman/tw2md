package ca.codepit.tw2md;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * @author evan
 */
public class DateTools {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
					.ofPattern("uuuuMMddHHmmssSSSXXXXX");

	public static Optional<LocalDateTime> ts(String s) {

		return zts(s).map(ZonedDateTime::toLocalDateTime);
	}

	public static Optional<ZonedDateTime> zts(String s) {

		try {
			ZonedDateTime zdtInstanceAtOffset = ZonedDateTime.parse(s + "+00:00", DATE_TIME_FORMATTER);
			ZonedDateTime zdtInstanceAtUTC = zdtInstanceAtOffset.withZoneSameInstant(ZoneOffset.systemDefault());
			return Optional.of(zdtInstanceAtUTC);
		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
