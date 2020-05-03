package org.onedatashare.transfer.repository;


import org.onedatashare.transfer.model.TransferDetails;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

@Configuration
public interface TransferDetailsRepository extends ReactiveMongoRepository<TransferDetails, String> {
}