package org.onedatashare.transfer.module.dropbox;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.*;
import org.onedatashare.transfer.model.core.*;
import org.onedatashare.transfer.model.drain.DbxDrain;
import org.onedatashare.transfer.model.drain.Drain;
import org.onedatashare.transfer.model.error.NotFoundException;
import org.onedatashare.transfer.model.tap.DropboxTap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Resource class that provides services specific to Dropbox endpoint.
 */
public class DbxResource extends Resource<DbxSession, DbxResource> {

  DbxResource(DbxSession session, String path) {
    super(session, path);
  }

  @Override
  public Mono<DbxResource> select(String name) {
    return getSession().select(name);
  }

  public Flux<String> list() {
    return initialize().flux().flatMap(resource -> {
      ListFolderResult listing = null;
      try {
        listing = getSession().getClient().files().listFolder(getPath());
      } catch (DbxException e) {
        e.printStackTrace();
      }
      return Flux.fromIterable(listing.getEntries()).map(Metadata::getName);
    });
  }

  public Mono<DbxResource> mkdir() {
    return initialize().doOnSuccess(resource -> {
      try {
        resource.getSession().getClient().files().createFolderV2(getPath());
      } catch (DbxException e) {
        e.printStackTrace();
      }
    });
  }

  public Mono<DbxResource> delete() {
    return initialize().map(resource -> {
      try {
        resource.getSession().getClient().files().deleteV2(getPath());
      } catch (DbxException e) {
        e.printStackTrace();
      }
      return resource;
    });
  }

  public Mono<Stat> stat() {
    return initialize().map(DbxResource::onStat);
  }

  public Stat onStat() {
    Stat stat = new Stat();
    ListFolderResult data = null;
    Metadata mData = null;
    try {
      if (getPath().equals("/")) {
        data = getSession().getClient().files().listFolder("");
      } else {
        try {
          data = getSession().getClient().files().listFolder(getPath());
        } catch (ListFolderErrorException e) {
          mData = getSession().getClient().files().getMetadata(getPath());
        }
      }
      if (data == null && mData == null)
        throw new NotFoundException();
      if (data == null) {
        stat = mDataToStat(mData);
      } else {
        if (!data.getEntries().isEmpty()) {
          stat = mDataToStat(data.getEntries().iterator().next());
        }
        stat.setDir(true);
        stat.setFile(false);
      }

      stat.setName(getPath());

      if (stat.isDir()) {
        ListFolderResult lfr = null;
        if (stat.getName().equals("/")) {
          lfr = getSession().getClient().files().listFolder("");
        } else {
          // If the metadata is a directory
          if (getSession().getClient().files().getMetadata(getPath()) instanceof FolderMetadata) {
            // list the directory files
            lfr = getSession().getClient().files().listFolder(getPath());
          }
          // If the metadata is a file
          else if (getSession().getClient().files().getMetadata(getPath()) instanceof FileMetadata) {
            // Return the metadata as a stat object
            stat = mDataToStat(getSession().getClient().files().getMetadata(getPath()));
          }
        }
        List<Stat> sub = new LinkedList<>();
        for (Metadata child : lfr.getEntries())
          sub.add(mDataToStat(child));
        stat.setFiles(sub);
      }
    } catch (DbxException e) {
      e.printStackTrace();
    }
    return stat;
  }

  @Override
  public Mono<Stat> getTransferStat(){
    return initialize()
            .map(DbxResource::onStat)
            .map(s ->{
              List<Stat> sub = new LinkedList<>();
              long directorySize = 0L;
              try{
                if(s.isDir())
                  directorySize = buildDirectoryTree(sub, getSession().getClient().files().listFolder(getPath()), "/");
                else{
                  setFileResource(true);
                  sub.add(s);
                  directorySize = s.getSize();
                }
              }
              catch (DbxException e) {
                e.printStackTrace();
              }
              s.setFilesList(sub);
              s.setSize(directorySize);
              return s;
            });
  }

  public Long buildDirectoryTree(List<Stat> sub, ListFolderResult lfr, String relativeDirName) throws DbxException{
    long directorySize = 0L;
    for(Metadata childNode : lfr.getEntries()){
      if(childNode instanceof FileMetadata){
        Stat fileStat = mDataToStat(childNode);
        fileStat.setName(relativeDirName + fileStat.getName());
        directorySize += fileStat.getSize();
        sub.add(fileStat);
      }
      else if(childNode instanceof FolderMetadata){
        directorySize += buildDirectoryTree(sub, getSession().getClient().files().listFolder(((FolderMetadata) childNode).getId()),
                                            relativeDirName + childNode.getName()+"/");
      }
    }
    return directorySize;
  }

  private Stat mDataToStat(Metadata data) {
    Stat stat = new Stat(data.getName());
    if (data instanceof FileMetadata) {
      FileMetadata file = (FileMetadata) data;
      stat.setFile(true);
      stat.setSize(file.getSize());
      stat.setTime(file.getClientModified().getTime() / 1000);
    }
    if (data instanceof FolderMetadata) {
      stat.setDir(true);
    }
    return stat;
  }

  public DropboxTap tap() {
    DropboxTap dropboxTap = new DropboxTap();
    return dropboxTap;
  }

  public DbxDrain sink() {
    return new DbxDrain().start();
  }

  public DbxDrain sink(Stat stat){
    return new DbxDrain().start(getPath() + stat.getName());
  }



  public Mono<String> generateDownloadLink(){
    String downloadLink="";
    try {
//      downloadLink = session.client.sharing().createSharedLinkWithSettings(path).getUrl();    // throws an exception if a shared link already exists
      downloadLink = getSession().getClient().files().getTemporaryLink(getPath()).getLink();    //temporary link valid for 4 hours

    }
    catch(DbxException dbxe){
//      ODSLoggerService.logError("Error encountered while generating shared link for " + getPath(), dbxe);
    }
    return Mono.just(downloadLink);
  }
}
