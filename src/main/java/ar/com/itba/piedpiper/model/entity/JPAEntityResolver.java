package ar.com.itba.piedpiper.model.entity;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.HibernateException;
import org.springframework.stereotype.Component;

@Component
public class JPAEntityResolver implements EntityResolver {

	@PersistenceContext
	private EntityManager entityManager;

	public JPAEntityResolver() {}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T fetch(Class<T> type, Serializable id) {
		try {
			// XXX: hack to avoid loading of Classes "classname_$$javassist_$$"
			// that are lazy collections!!
			if (type.getSimpleName().contains("javassist")) {
				String name = type.getCanonicalName();
				name = name.substring(0, name.indexOf("_$$"));
				type = (Class<T>) Class.forName(name);
			}
			return (T) entityManager.find(type, id);
		} catch (Exception ex) {
			throw new HibernateException("Problem while fetching (" + type.getSimpleName() + ", " + id.toString() + ")", ex);
		}
	}
}
