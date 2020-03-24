package com.oneDatashare.transfer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.beans.Transient;
import java.util.ArrayList;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class  UserActionResource {
    private String uri;
    private String id;
    private ArrayList<IdMap> map;
    private UserActionCredential credential;
    private String type;

    @Transient
    private UploadCredential uploader;
}