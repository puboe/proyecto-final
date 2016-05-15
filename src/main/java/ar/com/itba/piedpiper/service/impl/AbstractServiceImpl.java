package ar.com.itba.piedpiper.service.impl;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import ar.com.itba.piedpiper.model.entity.PersistentEntity;
import ar.com.itba.piedpiper.service.api.AbstractService;

public abstract class AbstractServiceImpl<E extends PersistentEntity<ID>, ID extends Serializable> implements AbstractService<E, ID> {

	@PersistenceContext
	private EntityManager entityManager;

	public abstract JpaRepository<E, ID> repository();

	public EntityManager em() {
		return entityManager;
	}

	@Override
	public Optional<E> findOne(ID id) {
		return Optional.fromNullable(repository().getOne(id));
	}

	@Override
	public Collection<E> findAll() {
		return repository().findAll();
	}

	@Override
	public E save(E entity) {
		return repository().save(entity);
	}

	@Override
	public Page<E> findAll(Pageable pageable) {
		return repository().findAll(pageable);
	}

	@Override
	public void deleteInBatch(Iterable<E> entities) {
		repository().deleteInBatch(entities);
	}

	@Override
	public void delete(E entity) {
		repository().delete(entity);
	}

	@Override
	public void delete(Iterable<? extends E> entities) {
		repository().delete(entities);
	}

	@Override
	public <S extends E> List<S> save(Iterable<S> entities) {
		if (Iterables.isEmpty(entities)) {
			return Lists.newLinkedList(entities);
		}
		// XXX: must be careful when using lazy evaluated lists (eg: guava)
		List<S> loadedList = new LinkedList<>();
		Iterables.addAll(loadedList, entities);
		return repository().save(loadedList);
	}

	@Transactional(propagation = Propagation.MANDATORY)
	protected <T> void batchInsert(Iterable<T> entities) {
		for (T entity : entities) {
			entityManager.persist(entity);
		}
	}

	@Override
	public void flushCaches() {
	}

	protected <T extends Iterable<?>> T nullIfEmpty(T collection) {
		if (collection == null) {
			return null;
		}
		if (collection instanceof Collection<?>) {
			return ((Collection<?>) collection).isEmpty() ? null : collection;
		}
		return Iterables.isEmpty((Iterable<?>) collection) ? null : collection;
	}

	protected Integer booleanToInt(Boolean bool) {
		if (bool == null) {
			return null;
		}
		return bool ? 1 : 0;
	}

	protected LocalDateTime toLocalDateTime(LocalDate localDate) {
		LocalTime time = LocalTime.MIDNIGHT;
		return localDate == null ? null : localDate.toLocalDateTime(time);
	}
	
	protected String simpleName(Class<?> clazz) {
		return clazz.getSimpleName();
	}

	protected Predicate match(Root<?> root, CriteriaBuilder cb, Object value, String attribute) {
		Path<?> path = root.get(attribute);
		return value == null ? path.isNull() : cb.equal(path, value);
	}
}
