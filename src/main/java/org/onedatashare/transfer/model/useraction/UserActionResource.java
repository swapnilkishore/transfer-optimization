package org.onedatashare.transfer.model.useraction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.ArrayList;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class  UserActionResource {
  private String uri;
  private String id;
  private UserActionCredential credential;
  private ArrayList<IdMap> map;
  private String type;

}