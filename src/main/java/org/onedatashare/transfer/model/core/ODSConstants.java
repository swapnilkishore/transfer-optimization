package org.onedatashare.transfer.model.core;

import edu.emory.mathcs.backport.java.util.Arrays;

import java.util.HashSet;
import java.util.Set;

public class ODSConstants {

    public static final String DROPBOX_URI_SCHEME = "dropbox:///";
    public static final String DRIVE_URI_SCHEME = "googledrive:/";
    public static final String BOX_URI_SCHEME = "box:///";
    public static final String SFTP_URI_SCHEME = "sftp://";
    public static final String FTP_URI_SCHEME = "ftp://";
    public static final String SCP_URI_SCHEME = "scp://";
    public static final String GRIDFTP_URI_SCHEME = "gsiftp://";
    public static final String HTTP_URI_SCHEME = "http://";
    public static final String HTTPS_URI_SCHEME = "https://";
    public static final String DROPBOX_CLIENT_IDENTIFIER = "OneDataShare-DIDCLab";

    public static final String COOKIE = "cookie";
    public static final String TOKEN_COOKIE_NAME = "ATOKEN";


    public static final long TRANSFER_SLICE_SIZE = 1<<20;

    //Token valid for 7 days
    public static final long JWT_TOKEN_EXPIRES_IN = 28800 * 7;

}