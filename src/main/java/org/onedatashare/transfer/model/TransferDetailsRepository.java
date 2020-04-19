package org.onedatashare.transfer.model;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

@Configuration
public interface TransferDetailsRepository extends ReactiveMongoRepository<TransferDetails, String> {
}
