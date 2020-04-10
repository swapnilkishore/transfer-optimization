package org.onedatashare.transfer.service;

import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.OAuthCredential;
import org.onedatashare.transfer.model.useraction.UserActionCredential;
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


    public OAuthCredential updateCredential(UserActionCredential userActionCredential, OAuthCredential credential) {
        //Updating the access token for googledrive using refresh token or deleting credential if refresh token is expired.
        getLoggedInUser()
                .doOnSuccess(user -> {
                    Map<UUID,Credential> credsTemporary = user.getCredentials();
                    UUID uid = UUID.fromString(userActionCredential.getUuid());
                    OAuthCredential val = (OAuthCredential) credsTemporary.get(uid);
                    if(credential.refreshTokenExp){
                        credsTemporary.remove(uid);
                    }else if(val.refreshToken != null && val.refreshToken.equals(credential.refreshToken)){
                        credsTemporary.replace(uid, credential);
                    }
                    if(user.isSaveOAuthTokens()) {
                        user.setCredentials(credsTemporary);
                        userRepository.save(user).subscribe();
                    }
                }).subscribe();

        return credential;
    }

    public Flux<UUID> getJobs() {
        return getLoggedInUser().map(User::getJobs).flux().flatMap(Flux::fromIterable);
    }
}