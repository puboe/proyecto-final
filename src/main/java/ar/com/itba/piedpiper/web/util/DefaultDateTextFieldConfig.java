package ar.com.itba.piedpiper.web.util;

import org.joda.time.DateTime;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.DateTextFieldConfig;

@SuppressWarnings("serial")
public class DefaultDateTextFieldConfig extends DateTextFieldConfig {

	public DefaultDateTextFieldConfig() {
		autoClose(true);
		withView(DateTextFieldConfig.View.Month);
		showTodayButton(TodayButton.TRUE);
		withStartDate(new DateTime().withYear(1900));
		// XXX: Set language through config
		withLanguage("en");
	}

}
