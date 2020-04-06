package org.onedatashare.transfer.model.request;

import lombok.Data;

@Data
public class PageRequest {
    public int pageNo;
    public int pageSize;
    public String sortBy;
    public String sortOrder;
}
