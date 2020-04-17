package org.onedatashare.transfer;

import org.apache.log4j.BasicConfigurator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TransferApplication {
  public static void main(String[] args) {
    BasicConfigurator.configure();
    SpringApplication.run(TransferApplication.class, args);
  }
}
