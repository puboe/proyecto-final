package ar.com.itba.piedpiper.model.entity;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.wicket.injection.Injector;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.util.Assert;

import com.google.common.base.Preconditions;

public class EntityModel<T> implements IModel<T> {

	private static final long serialVersionUID = 1L;

	public static <T extends PersistentEntity<?>> EntityModel<T> of(Class<T> type) {
		return new EntityModel<>(type);
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends PersistentEntity<?>> EntityModel<T> of(T value) {
		Preconditions.checkNotNull(value);
		return new EntityModel<>((Class<T>) value.getClass(), value);
	}
	
	public static <T extends PersistentEntity<?>> EntityModel<T> of(Class<T> type, T value) {
		return new EntityModel<>(type, value);
	}
	
	private Class<T> type;
	private Serializable id;

	private transient T value;
	private transient boolean attached;

	@SpringBean
	private transient EntityResolver resolver;

	public EntityModel(Class<T> type, T object) {
		super();
		Assert.notNull(type, "You must provide a type for entity resolver models!");
		this.type = type;
		this.id = (object == null ? null : ((PersistentEntity<?>) object).getId());
		this.value = object;
		this.attached = true;
	}

	public EntityModel(Class<T> type) {
		this(type, (T) null);
	}

	protected T load() {
		if (id == null) {
			return null;
		}
		return resolver().fetch(type, id);
	}

	@Override
	public T getObject() {
		if (!attached) {
			value = load();
			attached = true;
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void setObject(T object) {
		if (object instanceof EntityModel<?>) {
			object = ((EntityModel<T>) object).getObject(); 
		}
		if (object instanceof String) {
			return;
		}
		id = (object == null) ? null : ((PersistentEntity<?>) object).getId();
		value = object;
		attached = true;
	}

	private EntityResolver resolver() {
		if (resolver == null) {
			Injector.get().inject(this);
			Assert.state(resolver != null, "Can't inject entity resolver!");
		}
		return resolver;
	}

	@Override
	public void detach() {
		if (attached) {
			value = null;
			attached = false;
		}
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).append("class", this.type).append("id", id).toString();
	}
	
	@Override
	public int hashCode() {
		T object = getObject();
		if (object == null) {
			return 0;
		}
		return object.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || !(obj instanceof IModel<?>)) {
			return false;
		}
		return new EqualsBuilder().append(getObject(), ((IModel<?>) obj).getObject()).isEquals();
	}

}