package ar.com.itba.piedpiper.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import ar.com.itba.piedpiper.model.entity.Configuration;
import ar.com.itba.piedpiper.repository.ConfigurationRepository;
import ar.com.itba.piedpiper.service.api.ConfigurationService;

@Service
public class ConfigurationServiceImpl extends AbstractServiceImpl<Configuration, Integer> implements ConfigurationService {

	@Autowired
	ConfigurationRepository repo;

	@Override
	public JpaRepository<Configuration, Integer> repository() {
		return repo;
	}

	@Override
	public Configuration findByName(String name) {
		return repo.findByName(name);
	}

}
