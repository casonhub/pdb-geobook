package com.geobook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

public interface MultimediaRepository extends JpaRepository<Multimedia, Long> {
    @Query("SELECT m FROM Multimedia m WHERE LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Multimedia> searchByDescription(@Param("query") String query);
}
