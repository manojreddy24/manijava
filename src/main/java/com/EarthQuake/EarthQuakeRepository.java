package com.EarthQuake;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EarthQuakeRepository extends MongoRepository<EarthQuake, String> {
}
