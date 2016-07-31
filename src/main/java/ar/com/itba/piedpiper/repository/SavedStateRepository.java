package ar.com.itba.piedpiper.repository;

import org.joda.time.DateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ar.com.itba.piedpiper.model.entity.SavedState;

public interface SavedStateRepository extends JpaRepository<SavedState, Integer> {

	@Query("SELECT savedState FROM SavedState savedState WHERE dateTime = :dateTime AND steps = :steps")
	SavedState findOne(@Param("dateTime") DateTime dateTime, @Param("steps") int steps);
	
}
