package ar.com.itba.piedpiper.web.dataprovider.api;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;

import ar.com.itba.piedpiper.model.entity.EntityModel;

@SuppressWarnings("serial")
public abstract class AbstractDataProvider<T> implements IDataProvider<T> {

	public AbstractDataProvider() {
		Injector.get().inject(this);
	}

	@SuppressWarnings("unchecked")
	@Override
	public IModel<T> model(T object) {
		return new EntityModel<>((Class<T>) object.getClass(), object);
	}

}
