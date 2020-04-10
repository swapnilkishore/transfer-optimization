package org.onedatashare.transfer.service;

import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.*;

/**
 * Service class for all operations related to users' information.
 */
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<User> getUser(String email) {
         return userRepository.findById(email)
                 .switchIfEmpty(Mono.error(new Exception("No User found with Id: " + email)));
    }

    public Mono<User> saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Modified the function to use security context for logging in
     * @return User : The current logged in user
     */
    public Mono<User> getLoggedInUser() {
        return getLoggedInUserEmail()
                .flatMap(this::getUser);
    }


    /**
     * This function returns the email id of the user that has made the request.
     * This information is retrieved from security context set using JWT
     * @return email: Email id of the user making the request
     */
    public Mono<String> getLoggedInUserEmail(){
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> (String) securityContext.getAuthentication().getPrincipal());
    }

    public Flux<UUID> getJobs() {
        return getLoggedInUser().map(User::getJobs).flux().flatMap(Flux::fromIterable);
    }
}