package ar.com.itba.piedpiper.model.entity;

import java.io.Serializable;

/**
 * Simple entity resolver that delegates finding entities to the underlying
 * storage
 */
public interface EntityResolver {
	/**
	 * Fetch an entity
	 * 
	 * @param <T>
	 *            The type of the entity
	 * @param type
	 *            The type of the entity
	 * @param id
	 *            The identifier of the entity
	 * @return The entity requested
	 */
	<T> T fetch(Class<T> type, Serializable id);

}