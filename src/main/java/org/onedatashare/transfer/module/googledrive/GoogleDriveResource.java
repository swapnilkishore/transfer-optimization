package org.onedatashare.transfer.module.googledrive;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.credential.OAuthCredential;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.drain.GoogleDriveDrain;
import org.onedatashare.transfer.model.error.NotFoundException;
//import org.onedatashare.transfer.service.ODSLoggerService;
import org.onedatashare.transfer.model.tap.GoogleDriveTap;
import reactor.core.publisher.Mono;

import java.io.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Resource class that provides services for Google Drive endpoint.
 */
public class GoogleDriveResource extends Resource<GoogleDriveSession, GoogleDriveResource> {

    public static final String ROOT_DIR_ID = "root";

    protected GoogleDriveResource(GoogleDriveSession session, String path, String id) {
        super(session, path, id);
    }
    protected GoogleDriveResource(GoogleDriveSession session, String path) {
        super(session, path,null);
    }

    public Mono<GoogleDriveResource> mkdir() {
        return Mono.create(s -> {
            try {
                String[] currpath = getPath().split("/");
                for(int i =0; i<currpath.length; i++){
                    File fileMetadata = new File();
                    fileMetadata.setName(currpath[i]);
                    fileMetadata.setMimeType("application/vnd.google-apps.folder");
                    fileMetadata.setParents(Collections.singletonList(getId()));
                    File file = getSession().getService().files().create(fileMetadata)
                            .setFields("id")
                            .execute();
//                    ODSLoggerService.logInfo("Folder ID: " + file.getId());
                    setId(file.getId());
                }
            } catch (IOException e) {
                e.printStackTrace();
                s.error(e);
            }
            s.success(this);
        });
    }

    public String mkdir(String directoryTree[]){
        String curId = ROOT_DIR_ID;

        for(int i=1; i< directoryTree.length-1; i++){
            String exisitingID = folderExistsCheck(curId, directoryTree[i]);
            if(exisitingID == null){
                try {
                    File fileMetadata = new File();
                    fileMetadata.setName(directoryTree[i]);
                    fileMetadata.setMimeType("application/vnd.google-apps.folder");
                    fileMetadata.setParents(Collections.singletonList(curId));
                    File file = getSession().getService().files().create(fileMetadata)
                            .setFields("id")
                            .execute();
                    curId = file.getId();

                } catch (IOException ioe) {
//                    ODSLoggerService.logError("Exception encountered while creating directory tree", ioe);
                }
            }
            else{
                curId = exisitingID;
            }
        }
        return curId;
    }

    public String folderExistsCheck(String curId, String directoryName){

        try {
            String query = new StringBuilder().append("trashed=false and ").append("'" + curId + "'")
                                              .append(" in parents").toString();

            Drive.Files.List request = getSession().getService().files().list()
                    .setOrderBy("name").setQ(query)
                    .setFields("nextPageToken, files(id, name, kind, mimeType, size, modifiedTime)");
            FileList fileSet = null;
            List<File> fileList = null;

            do{
                fileSet = request.execute();
                fileList = fileSet.getFiles();

                for(File file : fileList){
                    if (file.getMimeType().equals("application/vnd.google-apps.folder")
                            && file.getName().equals(directoryName)) {
                        return file.getId();
                    }
                }
                request.setPageToken(fileSet.getNextPageToken());
            }while(request.getPageToken() != null);
        }
        catch (IOException ioe){
//            ODSLoggerService.logError("Exception encountered while checking if folder " + directoryName +
//                                        " exists in " + curId, ioe);
        }
        return null;
    }

    public Mono<GoogleDriveResource> delete() {
       return Mono.create(s -> {
           try {
               getSession().getService().files().delete(getId()).execute();
               setId( getSession().idMap.get(getSession().idMap.size() - 1).getId() );
               if(getId() == null && getSession().idMap.size() ==1)
                   setId(ROOT_DIR_ID);
           } catch (Exception e) {
               s.error(e);
           }
           s.success(this);
       });
    }

    public Mono<String> download(){
        String downloadUrl ="";
        try {
            downloadUrl = "https://drive.google.com/uc?id="+ getId() +"&export=download";

        }catch(Exception exp){
//            ODSLoggerService.logError("Error encountered while generating shared link for " + getPath(), exp);
        }
        return Mono.just(downloadUrl);
    }

    public Mono<Stat> stat() {
        return Mono.just(onStat());
    }

    public Stat onStat() {
        Drive.Files.List result ;
        Stat stat = new Stat();
        stat.setName(getPath());
        stat.setId(getId());

        try {
            if (getPath().equals("/")) {
                stat.setDir(true);
                result = getSession().getService().files().list()
                    .setOrderBy("name")
                    .setQ("trashed=false and 'root' in parents")
                    .setFields("nextPageToken, files(id, name, kind, mimeType, size, modifiedTime)");

                if (result == null)
                    throw new NotFoundException();

                FileList fileSet = null;
                List<Stat> sub = new LinkedList<>();
                do {
                    try {
                        fileSet = result.execute();
                        List<File> files = fileSet.getFiles();
                        for (File file : files) {
                            sub.add(mDataToStat(file));
                        }
                        stat.setFiles(sub);
                        result.setPageToken(fileSet.getNextPageToken());
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                        fileSet.setNextPageToken(null);
                    }
                }
                while (result.getPageToken() != null);
            } else {
                try {
                    File googleDriveFile = getSession().getService().files().get(getId())
                                                .setFields("id, name, kind, mimeType, size, modifiedTime")
                                                .execute();
                    if (googleDriveFile.getMimeType().equals("application/vnd.google-apps.folder")) {
                        stat.setDir(true);

                        String query = new StringBuilder().append("trashed=false and ")
                                                .append("'" + getId() + "'").append(" in parents").toString();
                        result = getSession().getService().files().list()
                                        .setOrderBy("name").setQ(query)
                                        .setFields("nextPageToken, files(id, name, kind, mimeType, size, modifiedTime)");
                        if (result == null)
                            throw new NotFoundException();

                        FileList fileSet = null;

                        List<Stat> sub = new LinkedList<>();
                        do {
                            try {
                                fileSet = result.execute();
                                List<File> files = fileSet.getFiles();
                                for (File file : files) {
                                    sub.add(mDataToStat(file));
                                }
                                stat.setFiles(sub);
                                result.setPageToken(fileSet.getNextPageToken());
                            } catch (NullPointerException e) {

                            } catch (Exception e) {
                                fileSet.setNextPageToken(null);
                            }
                        }
                        while (result.getPageToken() != null);
                    } else {
                        stat.setFile(true);
                        stat.setTime(googleDriveFile.getModifiedTime().getValue() / 1000);
                        stat.setSize(googleDriveFile.getSize());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return stat;
    }

    public Mono<Stat> getTransferStat(){
        return stat().map(s ->{
                   List<Stat> sub = new LinkedList<>();
                    long directorySize = 0L;

                    if(s.isDir()){
                        directorySize = buildDirectoryTree(sub, getId(), "/");
                    }
                    else{
                        sub.add(s);
                        directorySize = s.getSize();
                    }
                    s.setFilesList(sub);
                    s.setSize(directorySize);
                    return s;
                });
    }

    public Long buildDirectoryTree(List<Stat> sub, String curId, String relativePath){
        Long directorySize = 0L;
        try {
            String query = new StringBuilder().append("trashed=false and ").append("'" + curId + "'")
                    .append(" in parents").toString();

            Drive.Files.List request = getSession().getService().files().list()
                    .setOrderBy("name").setQ(query)
                    .setFields("nextPageToken, files(id, name, kind, mimeType, size, modifiedTime)");
            FileList fileSet = null;
            List<File> fileList = null;

            do{
                fileSet = request.execute();
                fileList = fileSet.getFiles();

                for(File file : fileList){
                    if (file.getMimeType().equals("application/vnd.google-apps.folder")) {
                        directorySize += buildDirectoryTree(sub, file.getId(), relativePath + file.getName() + "/");
                    }
                    else{
                        Stat fileStat = mDataToStat(file);
                        fileStat.setName( relativePath + file.getName());
                        directorySize += fileStat.getSize();
                        sub.add(fileStat);
                    }
                }
                request.setPageToken(fileSet.getNextPageToken());
            }while(request.getPageToken() != null);
        }
        catch (IOException ioe){
//            ODSLoggerService.logError("Exception encountered while building directory tree", ioe);
        }
        return directorySize;
    }

    private Stat mDataToStat(File file) {
        Stat stat = new Stat(file.getName());

        try {
            stat.setFile(true);
            stat.setId(file.getId());
            stat.setTime(file.getModifiedTime().getValue()/1000);
            if (file.getMimeType().equals("application/vnd.google-apps.folder")) {
                stat.setDir(true);
                stat.setFile(false);
            }
            else if(file.containsKey("size"))
                stat.setSize(file.getSize());
        }
        catch (NullPointerException  e) {

        }catch (Exception  e) {
          e.printStackTrace();
        }
        return stat;
    }

    public GoogleDriveTap tap() {
        GoogleDriveTap gDriveTap = new GoogleDriveTap();
        return gDriveTap;
    }


    public GoogleDriveDrain sink() {
        return new GoogleDriveDrain().start();
    }

    public GoogleDriveDrain sink(Stat stat){
        return new GoogleDriveDrain().start(getPath() + stat.getName());
    }

    @Override
    public Mono<GoogleDriveResource> select(String path) {
        return null;
    }
}
