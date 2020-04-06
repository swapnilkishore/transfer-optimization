package org.onedatashare.transfer.model.credential;

import lombok.Data;
import org.onedatashare.transfer.model.core.Credential;
import org.onedatashare.transfer.model.useraction.UserActionCredential;
import org.springframework.data.annotation.Transient;

@Data
public class UserInfoCredential extends Credential {
  private String username;
  @Transient
  private String password;

  public UserInfoCredential(String username, String password) {
    this.type = CredentialType.USERINFO;
    this.username = username;
    this.password = password;
  }

  public UserInfoCredential(UserActionCredential credential) {
    this(null, null);
    if(credential != null){
      this.username = credential.getUsername();
      this.password = credential.getPassword();
    }
  }
}
