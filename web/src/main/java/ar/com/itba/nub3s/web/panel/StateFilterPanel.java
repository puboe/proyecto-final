package ar.com.itba.nub3s.web.panel;

import java.io.Serializable;
import java.util.Date;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.datetime.markup.html.form.DateTextField;
import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import ar.com.itba.nub3s.model.entity.Channel;
import ar.com.itba.nub3s.web.util.EnumDropDownChoice;
import de.agilecoders.wicket.core.markup.html.bootstrap.components.TooltipBehavior;

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
			.add(new EnumDropDownChoice("channel", resourceComponent, Channel.values(), false).setDefaultModel(model.channelModel()))
			.add(new DateTimeField("to", model.toModel()){
				@Override
				protected boolean use12HourFormat() {
					return false;
				}
				
				@Override
				protected DateTextField newDateTextField(String id, PropertyModel<Date> dateFieldModel) {
					return DateTextField.forDatePattern(id, dateFieldModel, "dd-MM-yyyy");
				};
				
			}.add(new TooltipBehavior(Model.of("Fecha y hora en 24hr"))))
			.add(new TextField<>("steps", model.stepsModel()).add(new TooltipBehavior(Model.of("Cantidad de pasos hacia atras"))))
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
		
		private IModel<Enum<?>> channelModel;
		private IModel<Date> toDateModel;
		private IModel<String> stepsModel;
		private IModel<Boolean> enhancedModel;
		
		public StateFilterModel(Channel channel, Date date, int steps, boolean enhanced) {
			channelModel = Model.of(channel);
			toDateModel = Model.of(date);
			stepsModel = Model.of(String.valueOf(steps));
			enhancedModel = Model.of(enhanced);
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
