package ar.com.itba.piedpiper.web.util;

import java.io.Serializable;
import java.util.Date;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import com.google.common.base.Optional;

@SuppressWarnings("serial")
public class DateRange implements Serializable {

	private static final Optional<DateRange> optionalDateRange = Optional.absent();

	public static Optional<DateRange> absent() {
		return optionalDateRange;
	}
	
	private LocalDate from;
	private LocalDate to;

	public DateRange() {
		this((LocalDate) null, (LocalDate) null);
	}

	public DateRange(Date from, Date to) {
		this(
			from == null ? null : new LocalDate(from), 
			to == null ? null : new LocalDate(to)
		);
	}

	public DateRange(LocalDate from, LocalDate to) {
		this.from = from;
		this.to = to;
	}

	public LocalDate to() {
		return to;
	}

	public LocalDateTime toAsLocalDateTime() {
		return to == null ? null : to.toLocalDateTime(LocalTime.MIDNIGHT);
	}
	
	public LocalDateTime fromAsLocalDateTime() {
		return from == null ? null : from.toLocalDateTime(LocalTime.MIDNIGHT);
	}
	
	public LocalDate from() {
		return from;
	}

	public boolean isRangeNull() {
		return from == null || to == null;
	}
}
