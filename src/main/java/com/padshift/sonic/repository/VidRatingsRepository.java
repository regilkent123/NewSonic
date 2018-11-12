package com.padshift.sonic.repository;

import com.padshift.sonic.entities.VidRatings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Created by Regil on 10/11/2018.
 */
@Repository("vidRatingsRepository")
public interface VidRatingsRepository extends JpaRepository<VidRatings,Long> {
    VidRatings findByUserIdAndVideoid(String userId, String videoid);
}
