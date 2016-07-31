package ar.com.itba.piedpiper.repository;

import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.com.itba.piedpiper.model.entity.SavedState;

public interface SavedStateRepository extends JpaRepository<SavedState, Integer> {

	@Query("SELECT savedSate from SavedSate savedSate WHERE dateTime = :dateTime")
	SavedState findByDateTime(@Param("dateTime") DateTime dateTime);

}
