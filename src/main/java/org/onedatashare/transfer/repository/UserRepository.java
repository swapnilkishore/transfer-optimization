package org.onedatashare.transfer.repository;

import org.onedatashare.transfer.model.core.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface UserRepository extends ReactiveMongoRepository<User, String>{
}
