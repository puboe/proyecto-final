package ar.com.itba.piedpiper.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.com.itba.piedpiper.model.entity.Configuration;

public interface ConfigurationRepository extends JpaRepository<Configuration, Integer> {

	@Query("SELECT configuration from Configuration configuration WHERE name = :name")
	Configuration findByName(@Param("name") String name);

}
