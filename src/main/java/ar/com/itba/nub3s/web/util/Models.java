package ar.com.itba.nub3s.web.util;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.model.StringResourceModel;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class Models {

	public static StringResourceModel translate(String key, Component component) {
		return new StringResourceModel(key, component);
	}

	public static IModel<String> translate(Enum<?> value, Component component) {
		return translate(value.getClass().getSimpleName() + "." + value.name(), component);
	}

	public static IModel<String> translate(Boolean value, Component component) {
		value = value == null ? false : value;
		return translate(value.toString(), component);
	}

	public static IModel<String> translate(String key) {
		return new ResourceModel(key, "_" + key);
	}

	public static <T, F extends IModel<T>> List<T> outOfModel(List<F> models) {
		List<T> objects = Lists.newLinkedList();
		for (IModel<T> model : models) {
			objects.add(model.getObject());
		}
		return objects;
	}

	public static <F, T extends IModel<F>> Function<T, F> outOfModel() {
		return new Function<T, F>() {
			public F apply(T from) {
				return from.getObject();
			}
		};
	}

}
