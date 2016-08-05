package ar.com.itba.piedpiper.web.panel;

import java.io.Serializable;
import java.util.Date;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.CheckBox;
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

	private AjaxSubmitLink submit;
	
	public StateFilterPanel(String id, StateFilterModel model, Component resourceComponent) {
		super(id);
		Form<Integer> form = new Form<>("form");
		submit = new AjaxSubmitLink("search") {
			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				onSearch(target);
			}
		};
		form
			.add(new EnumDropDownChoice("channel", resourceComponent, Channel.values()) {
				@Override
				public boolean isNullValid() {
					return false;
				};
			}.setDefaultModel(model.channelModel()))
			.add(new DateTimeField("to", model.toModel()))
			.add(new TextField<>("steps", model.stepsModel()))
			.add(new CheckBox("enhanced", model.enhancedModel()) {
			})
			.add(submit);
	add(form);
	}
	
	public abstract void onSearch(AjaxRequestTarget target);
	
	public AjaxSubmitLink submit() {
		return submit;
	}
	
	public static final class StateFilterModel implements Serializable {
		
		private IModel<Enum<?>> channelModel = Model.of(Channel.IR2);
		private IModel<Date> toDateModel = Model.of(new LocalDate().toDate());
		private IModel<String> stepsModel;
		private IModel<Boolean> enhancedModel = Model.of(false);
		
		public StateFilterModel(String steps) {
			stepsModel = Model.of(steps);
		}
		
		public IModel<Enum<?>> channelModel() {
			return channelModel;
		}
		
		public Channel channelModelObject() {
			return (Channel) channelModel.getObject();
		}
		
		public IModel<Date> toModel() {
			return toDateModel;
		}
		
		public Date toModelObject() {
			return toDateModel.getObject();
		}

		public IModel<String> stepsModel() {
			return stepsModel;
		}
		
		public String stepsModelObject() {
			return stepsModel.getObject();
		}
		
		public IModel<Boolean> enhancedModel() {
			return enhancedModel;
		}
		
		public Boolean enhancedModelObject() {
			return enhancedModel.getObject();
		}
		
	}
}
