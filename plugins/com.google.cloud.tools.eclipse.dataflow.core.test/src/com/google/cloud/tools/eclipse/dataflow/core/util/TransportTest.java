package com.google.cloud.tools.eclipse.dataflow.core.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.api.services.storage.Storage;

public class TransportTest {

  @Test
  public void testNewStorageClient() throws CouldNotCreateCredentialsException {
    Storage.Builder builder = Transport.newStorageClient();
  }

}
