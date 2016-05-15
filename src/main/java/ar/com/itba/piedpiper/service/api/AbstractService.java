package ar.com.itba.piedpiper.service.api;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.google.common.base.Optional;

import ar.com.itba.piedpiper.model.entity.PersistentEntity;

public interface AbstractService<E extends PersistentEntity<ID>, ID extends Serializable> {

	Optional<E> findOne(ID id);

	Collection<E> findAll();

	E save(E entity);

	Page<E> findAll(Pageable pageable);

	void deleteInBatch(Iterable<E> entities);

	void delete(E entity);

	void delete(Iterable<? extends E> entities);

	<S extends E> List<S> save(Iterable<S> entities);

	void flushCaches();

}
