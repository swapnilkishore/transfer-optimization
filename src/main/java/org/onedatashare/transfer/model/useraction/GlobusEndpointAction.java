package org.onedatashare.transfer.model.useraction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GlobusEndpointAction {
    public String action;
    public String filter_fulltext;
}
