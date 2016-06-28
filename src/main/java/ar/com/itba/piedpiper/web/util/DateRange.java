package ar.com.itba.piedpiper.web.util;

import java.io.Serializable;
import java.util.Date;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.google.common.base.Optional;

@SuppressWarnings("serial")
public class DateRange implements Serializable {

	private static final Optional<DateRange> optionalDateRange = Optional.absent();

	public static Optional<DateRange> absent() {
		return optionalDateRange;
	}
	
	private LocalDateTime from;
	private LocalDateTime to;

	public DateRange() {
		this((LocalDateTime) null, (LocalDateTime) null);
	}

	public DateRange(Date from, Date to) {
		this(
			from == null ? null : new LocalDateTime(from), 
			to == null ? null : new LocalDateTime(to)
		);
	}

	public DateRange(LocalDateTime from, LocalDateTime to) {
		this.from = from;
		this.to = to;
	}

	public LocalDateTime to() {
		return to;
	}

	public LocalDate toAsLocalDate() {
		return to == null ? null : to.toLocalDate();
	}
	
	public LocalDate fromAsLocalDate() {
		return from == null ? null : from.toLocalDate();
	}
	
	public LocalDateTime from() {
		return from;
	}

	public boolean isRangeNull() {
		return from == null || to == null;
	}
}
