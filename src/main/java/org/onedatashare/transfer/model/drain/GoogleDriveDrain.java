package org.onedatashare.transfer.model.drain;

import org.onedatashare.transfer.model.core.Slice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class GoogleDriveDrain implements Drain {
    ByteArrayOutputStream chunk = new ByteArrayOutputStream();
    long size = 0;
    String resumableSessionURL;

    String drainPath;// = getPath();
    Boolean isDirTransfer = false;

    Logger logger = LoggerFactory.getLogger(GoogleDriveDrain.class);

//    @Override
    public GoogleDriveDrain start(String drainPath) {
        this.drainPath = drainPath;
        this.isDirTransfer = true;
        return start();
    }

//    @Override
    public GoogleDriveDrain start() {
        try{
            String name[] = drainPath.split("/");

//            if(isDirTransfer)
//                setId(mkdir(name));
//            else
//                setId( getSession().idMap.get(getSession().idMap.size()-1).getId() );

            URL url = new URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=resumable");
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.setRequestMethod("POST");
            request.setDoInput(true);
            request.setDoOutput(true);
//            request.setRequestProperty("Authorization", "Bearer " + ((OAuthCredential)(getSession().getCredential())).getToken());
            request.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            String body = ""; //changed
//            if(getId() !=null){
//                body = "{\"name\": \"" + name[name.length-1] + "\", \"parents\": [\"" + getId() + "\"]}";
//            }else{
//                body = "{\"name\": \"" + name[name.length-1] + "\"}";
//            }

            request.setRequestProperty("Content-Length", String.format(Locale.ENGLISH, "%d", body.getBytes().length));

            OutputStream outputStream = request.getOutputStream();
            outputStream.write(body.getBytes());
            outputStream.close();
            request.connect();
            int uploadRequestResponseCode  = request.getResponseCode();
            if(uploadRequestResponseCode == 200) {
                resumableSessionURL = request.getHeaderField("location");
            }else{
                throw new Exception("Transfer will fail");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public void drain(Slice slice) {
        try{
            chunk.write(slice.asBytes());

            // Google drive only supports 258KB (1 << 18) of data transfer per request
            int chunks = chunk.size() / (1<<18);
            int sizeUploading = chunks * (1<<18);

            URL url = new URL(resumableSessionURL);
            if(sizeUploading > 0) {
                HttpURLConnection request = (HttpURLConnection) url.openConnection();
                request.setRequestMethod("PUT");
                request.setConnectTimeout(10000);
                request.setRequestProperty("Content-Length", Long.toString(sizeUploading));
                request.setRequestProperty("Content-Range", "bytes " + size + "-" + (size + sizeUploading - 1) + "/" + "*");
                request.setDoOutput(true);
                OutputStream outputStream = request.getOutputStream();
                outputStream.write(chunk.toByteArray(), 0, sizeUploading);
                outputStream.close();
                request.connect();

                if (request.getResponseCode() == 308) {
                    size = size + sizeUploading;
                    ByteArrayOutputStream temp = new ByteArrayOutputStream();
                    temp.write(chunk.toByteArray(), sizeUploading, (chunk.size() - sizeUploading));
                    chunk = temp;
                } else if (request.getResponseCode() == 200 || request.getResponseCode() == 201) {
                        logger.debug("code: " + request.getResponseCode() +
                                ", message: " + request.getResponseMessage());
                } else {
                        logger.debug("code: " + request.getResponseCode() +
                                ", message: " + request.getResponseMessage());
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        try{
            URL url = new URL(resumableSessionURL);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            request.setRequestMethod("PUT");
            request.setConnectTimeout(10000);
            request.setRequestProperty("Content-Length", Long.toString(chunk.size()));
            if(chunk.size() == 0)
                request.setRequestProperty("Content-Range", "bytes */" + (size+chunk.size()));
            else
                request.setRequestProperty("Content-Range", "bytes " + size + "-" + (size+chunk.size()-1) + "/" + (size + chunk.size()));
            request.setDoOutput(true);
            OutputStream outputStream = request.getOutputStream();
            outputStream.write(chunk.toByteArray(), 0, chunk.size());
            outputStream.close();
            request.connect();
            if(request.getResponseCode() == 200 || request.getResponseCode() == 201){
//                    ODSLoggerService.logDebug("code: " + request.getResponseCode()+
//                            ", message: "+ request.getResponseMessage());
            }else {
//                    ODSLoggerService.logDebug("code: " + request.getResponseCode()+
//                            ", message: "+ request.getResponseMessage());
//                    ODSLoggerService.logDebug("fail");
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}

