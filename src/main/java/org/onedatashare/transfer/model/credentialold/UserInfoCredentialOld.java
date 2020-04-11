package org.onedatashare.transfer.model.credentialold;

import lombok.Data;
import org.onedatashare.transfer.model.core.CredentialOld;
import org.onedatashare.transfer.model.useraction.UserActionCredential;
import org.springframework.data.annotation.Transient;

@Data
public class UserInfoCredentialOld extends CredentialOld {
  private String username;
  @Transient
  private String password;

  public UserInfoCredentialOld(String username, String password) {
    this.type = CredentialType.USERINFO;
    this.username = username;
    this.password = password;
  }

  public UserInfoCredentialOld(UserActionCredential credential) {
    this(null, null);
    if(credential != null){
      this.username = credential.getUsername();
      this.password = credential.getPassword();
    }
  }
}
