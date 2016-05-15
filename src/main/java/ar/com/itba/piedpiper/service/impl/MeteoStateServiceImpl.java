package ar.com.itba.piedpiper.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;

import ar.com.itba.piedpiper.model.entity.MeteoState;
import ar.com.itba.piedpiper.repository.MeteoStateRepository;
import ar.com.itba.piedpiper.service.api.MeteoStateService;

@Service
public class MeteoStateServiceImpl extends AbstractServiceImpl<MeteoState, Integer> implements MeteoStateService {

	@Autowired
	MeteoStateRepository repo;

	@Override
	public Optional<MeteoState> findByFilename(String filename) {
		return Optional.fromNullable(repo.findByFilename(filename));
	}
	
	@Override
	public Page<MeteoState> suggest(String input, Pageable pageable) {
		return repo.suggest(input, pageable);
	}
	
	@Override
	public JpaRepository<MeteoState, Integer> repository() {
		return repo;
	}
	
}
