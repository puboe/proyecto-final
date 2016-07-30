package ar.com.itba.piedpiper.web.panel;

import java.io.Serializable;
import java.util.Date;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.joda.time.LocalDate;

import ar.com.itba.piedpiper.model.entity.Channel;
import ar.com.itba.piedpiper.web.util.EnumDropDownChoice;

@SuppressWarnings("serial")
public abstract class StateFilterPanel extends Panel {

	public StateFilterPanel(String id, StateFilterModel model, Component resourceComponent) {
		super(id);
		Form<Integer> form = new Form<>("form");
		form
			.add(new EnumDropDownChoice("channel", resourceComponent, Channel.values()).setDefaultModel(model.channelModel()))
			.add(new DateTimeField("to", model.toModel()))
			.add(new TextField<>("steps", model.stepsModel()))
			.add(new AjaxSubmitLink("search") {
				@Override
				protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
					onSearch(target);
				}
		});
	add(form);
	}
	
	public abstract void onSearch(AjaxRequestTarget target);
	
	public static final class StateFilterModel implements Serializable {
		
		private IModel<Enum<?>> channelModel = Model.of();
		private IModel<Date> toDateModel = Model.of(new LocalDate().toDate());
		private IModel<String> stepsModel = Model.of();
	
		public IModel<Enum<?>> channelModel(){
			return channelModel;
		}
		
		public IModel<Date> toModel() {
			return toDateModel;
		}

		public IModel<String> stepsModel() {
			return stepsModel;
		}
	}
}
