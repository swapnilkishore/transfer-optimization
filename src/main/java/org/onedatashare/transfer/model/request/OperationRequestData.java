package org.onedatashare.transfer.model.request;

import lombok.Data;
import org.onedatashare.transfer.model.useraction.IdMap;

import java.util.ArrayList;

@Data
public class OperationRequestData extends RequestData{
    private ArrayList<IdMap> map;
}
