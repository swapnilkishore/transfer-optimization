package org.onedatashare.transfer.model.drain;

import com.box.sdk.*;
import org.onedatashare.transfer.model.core.Slice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.onedatashare.transfer.model.core.ODSConstants.BOX_URI_SCHEME;

public class BoxDrain implements Drain {
    ByteArrayOutputStream chunk = new ByteArrayOutputStream();
    long totalSize = 0;
    long sizeUploaded = 0;
    int part_size;
    String fileName;

    String drainPath ;//= getPath();
    Boolean isDirTransfer = false;


    BoxFileUploadSession.Info sessionInfo;
    BoxFileUploadSession session;

    MessageDigest sha1;
    List<BoxFileUploadSessionPart> parts;

    ByteArrayInputStream smallFileStream;
    boolean isSmall;

    public BoxDrain start(String drainPath, long size, boolean isDir){
        totalSize = size;
        this.isDirTransfer = isDir;
        isSmall = (totalSize < 20971520) ? true : false;
        try{
            sha1 = MessageDigest.getInstance("SHA-1");
        }
        catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return start(drainPath);
    }

    @Override
    public BoxDrain start(String drainPath) {
        this.drainPath = drainPath;
        return start();
    }

    @Override
    public BoxDrain start() {

        String name = drainPath.substring(drainPath.lastIndexOf('/')+1);

        fileName = name;
//        try {
//            String parentid = getSession().idMap.get(getSession().idMap.size()-1).getId();
//            if( parentid != null ) {
//                setId( getSession().idMap.get(getSession().idMap.size()-1).getId() );
//            }else {
//                parentid = "0";
//                setId("0");
//            }
//
//            if(isDirTransfer) {
//                String path = drainPath.substring(BOX_URI_SCHEME.length(), drainPath.lastIndexOf("/"));
//                if(!hashMap.containsKey(path)){
//                    //directory has not been created in previous iterations
//                    String[] folders = path.split("/");
//                    BoxFolder parentFolder = new BoxFolder(getSession().client, parentid);
//                    for(String folder : folders){
//                        if(!folder.equals(parentFolder.getInfo().getName())) {
//                            BoxFolder.Info childFolder = parentFolder.createFolder(folder);
//                            parentid = childFolder.getID();
//                            parentFolder = childFolder.getResource();
//                        }
//                    }
//                    hashMap.put(path, parentid);
//                    setId(parentid);
//                }else {
//                    parentid = hashMap.get(path);
//                    setId(parentid);
//                }
//            }
//
//            BoxFolder folder = new BoxFolder(getSession().client, parentid);
//            if(!isSmall){
//                try {
//                    sessionInfo = folder.createUploadSession(name, totalSize);
//                    parts = new ArrayList<BoxFileUploadSessionPart>();
//                    session = sessionInfo.getResource();
//                } catch(Exception e){
//                    //Already Exists
//                }
//            }
//
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        return this;
    }

    @Override
    public void drain(Slice slice) {
        try {
            //Box only allows chunked upload for files greater than 20MB at 8MB chunks
            if (isSmall) {
                chunk.write(slice.asBytes());
            } else {
                try {
                    part_size = sessionInfo.getPartSize();
                } catch(NullPointerException npe){

                }
                chunk.write(slice.asBytes());
                if (chunk.size() == part_size) {
                    try {
                        BoxFileUploadSessionPart part = session.uploadPart(chunk.toByteArray(), sizeUploaded, chunk.size(), totalSize);
                        parts.add(part);
                        sizeUploaded = sizeUploaded + chunk.size();
                        sha1.update(chunk.toByteArray());
                        chunk = new ByteArrayOutputStream();
                    }catch(Exception e){
                        //Part already exists
                    }
                }

            }
        }catch(Exception e){
            e.printStackTrace();
        }

    }


    @Override
    public void finish() {
        try {
//            if (isSmall) {
//                BoxFolder folder = new BoxFolder(getSession().client, getId());
//                Iterable<BoxItem.Info> children = folder.getChildren();
//                for (BoxItem.Info child : children) {
//                    if(child.getName().equals(fileName)){
//                        return;
//                    }
//                }
//
//                smallFileStream = new ByteArrayInputStream(chunk.toByteArray());
//                BoxFile.Info smallFile = folder.uploadFile(smallFileStream, fileName);
//                smallFileStream.close();
//
//            } else {
//                if (chunk.size() > 0) {
//                    try {
//                        BoxFileUploadSessionPart part = session.uploadPart(chunk.toByteArray(), sizeUploaded, chunk.size(), totalSize);
//                        parts.add(part);
//                        sizeUploaded = sizeUploaded + chunk.size();
//                        sha1.update(chunk.toByteArray());
//                    }catch(Exception e){
//
//                    }
//                }
//
//                byte[] digestBytes = sha1.digest();
//
//                //Base64 encoding of the hash
//                String digestStr = Base64.getEncoder().encodeToString(digestBytes);
//                try {
//                    BoxFile.Info largeFile = session.commit(digestStr, parts, null, null, null);
//                }catch(Exception e){
//
//                }
//            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
