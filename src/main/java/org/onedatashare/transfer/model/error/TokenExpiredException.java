package org.onedatashare.transfer.model.error;

import org.onedatashare.transfer.model.credentialold.OAuthCredentialOld;
import org.springframework.http.HttpStatus;

public class TokenExpiredException extends ODSError {

    public OAuthCredentialOld cred;
    public TokenExpiredException(OAuthCredentialOld cred, String message) {
        super(message);
        type = "TokenExpired";
        error = "Token has expired.";
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
        this.cred = cred;
    }
}