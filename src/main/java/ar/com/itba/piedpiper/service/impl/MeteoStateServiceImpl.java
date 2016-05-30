package ar.com.itba.piedpiper.service.impl;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import ar.com.itba.piedpiper.model.entity.MeteoState;
import ar.com.itba.piedpiper.repository.MeteoStateRepository;
import ar.com.itba.piedpiper.service.api.JpaSpecificationService;
import ar.com.itba.piedpiper.service.api.MeteoStateService;

@Service
public class MeteoStateServiceImpl extends AbstractServiceImpl<MeteoState, Integer> implements MeteoStateService, JpaSpecificationService<MeteoState>  {

	@Autowired
	MeteoStateRepository repo;

	@Override
	public Optional<MeteoState> findByFilename(String filename) {
		return Optional.fromNullable(repo.findByFilename(filename));
	}
	
	@Override
	public Page<MeteoState> suggest(String input, Pageable pageable) {
		return Strings.isNullOrEmpty(input) ? repo.findAll(pageable) : repo.suggest(input, pageable);
	}
	
	@Override
	public MeteoState getLast() {
		return specificationExecutor().findAll(new Specification<MeteoState>() {
			@Override
			public Predicate toPredicate(Root<MeteoState> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return query.orderBy(cb.desc(root.get("date"))).getRestriction();
			}
		}).get(0);
	}
	
	@Override
	public JpaRepository<MeteoState, Integer> repository() {
		return repo;
	}

	@Override
	public JpaSpecificationExecutor<MeteoState> specificationExecutor() {
		return repo;
	}

}
