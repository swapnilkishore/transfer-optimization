package org.onedatashare.transfer.model.useraction;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.onedatashare.transfer.model.request.*;

import java.util.ArrayList;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserAction {
    // For selecting action on the endpoint
    private String action;

    // For handling registration
    private String firstName;
    private String lastName;
    private String organization;
    private String captchaVerificationValue;

    // For handling endpoint login
    private String username;

    // User Admin Flag
    private boolean isAdmin;

    // For handling login and password change
    private String email;
    private String password;
    private String confirmPassword;
    private String newPassword;
    private String code;

    // User preferences
    private boolean saveOAuth;
    private boolean compactViewEnabled;

    private String uuid;

    // For handling endpoint list and file operations
    private String type;
    private String uri;
    private String id;
    private String portNumber;
    private UserActionCredential credential;
    private ArrayList<IdMap> map;

    // For handling transfers
    private UserActionResource src;
    private UserActionResource dest;
    private TransferOptions options;

    // For queue page
    private Integer job_id;
    private int pageNo;
    private int pageSize;
    private String sortBy;
    private String sortOrder;

    // Misc
    private String filter_fulltext;

    /**
     * Factory method for returning an object of type request data
     * @param jobRequestData - data for making a job request
     * @return UserAction
     */
    public static UserAction convertToUserAction(JobRequestData jobRequestData){
        UserAction ua = new UserAction();
        ua.setJob_id(jobRequestData.getJob_id());
        return ua;
    }

    /**
     * Factory method for returning an object of type request data
     * @param transferRequest - data for making a transfer request
     * @return UserAction
     */
    public static UserAction convertToUserAction(TransferRequest transferRequest){
        UserAction ua = new UserAction();
        ua.setSrc(transferRequest.getSrc());
        ua.setDest(transferRequest.getDest());
        ua.setOptions(transferRequest.getOptions());
        return ua;
    }

}
