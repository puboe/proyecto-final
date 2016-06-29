package ar.com.itba.piedpiper.web.util;

import java.io.Serializable;
import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Optional;

@SuppressWarnings("serial")
public class DateRange implements Serializable {

	private static final Optional<DateRange> optionalDateRange = Optional.absent();

	public static Optional<DateRange> absent() {
		return optionalDateRange;
	}
	
	private DateTime from;
	private DateTime to;

	public DateRange() {
		this((DateTime) null, (DateTime) null);
	}

	public DateRange(Date from, Date to) {
		this(
			from == null ? null : new DateTime(from), 
			to == null ? null : new DateTime(to)
		);
	}

	public DateRange(DateTime from, DateTime to) {
		this.from = from;
		this.to = to;
	}

	public DateTime to() {
		return to;
	}

	public LocalDate toAsLocalDate() {
		return to == null ? null : to.toLocalDate();
	}
	
	public LocalDate fromAsLocalDate() {
		return from == null ? null : from.toLocalDate();
	}
	
	public DateTime from() {
		return from;
	}

	public boolean isRangeNull() {
		return from == null || to == null;
	}
}
