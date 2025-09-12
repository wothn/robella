package org.elmo.robella.repository;

import org.elmo.robella.model.entity.Model;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ModelRepository extends R2dbcRepository<Model, Long> {
    Flux<Model> findByPublishedTrue();
    Mono<Model> findByName(String name);
    Flux<Model> findByOrganization(String organization);
    Flux<Model> findByOrganizationAndPublishedTrue(String organization);
}