package ar.com.itba.piedpiper.web.util;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;

@SuppressWarnings("serial")
public class EnumDropDownChoice extends DropDownChoice<Enum<?>> {

	public EnumDropDownChoice(String id, Component resourceSource, Enum<?>[] choices) {
		this(id, resourceSource, choices, true);
	}

	public EnumDropDownChoice(String id, Component resourceSource, Enum<?>[] choices, boolean emptyIsvalid) {
		this(id, resourceSource, Arrays.asList(choices), emptyIsvalid);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public EnumDropDownChoice(String id, Component resourceSource, List<? extends Enum<?>> choices, boolean emptyIsvalid) {
		super(id, choices, new EnumChoiceRenderer(resourceSource));
		setNullValid(emptyIsvalid);
	}

	@Override
	protected CharSequence getDefaultChoice(String selectedValue) {
		return isNullValid() ? super.getDefaultChoice(selectedValue) : "";
	}
	
}
