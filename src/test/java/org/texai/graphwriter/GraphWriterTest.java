/*
 * GraphWriterTest.java
 *
 * Created on Jun 30, 2008, 8:58:26 AM
 *
 * Description: .
 *
 * Copyright (C) Aug 10, 2011 reed.
 *
 */
package org.texai.graphwriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.Scanner;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.texai.util.StringUtils;
import org.texai.util.TexaiException;

/**
 *
 * @author reed
 */
public class GraphWriterTest {

  // the logger, which is configured via log4j.properties to log to the file GraphWriter.log
  private static final Logger LOGGER = Logger.getLogger(GraphWriterTest.class);

  public GraphWriterTest() {
  }

  @BeforeClass
  public static void setUpClass() throws Exception {
    // shut down the GraphWriter server if it is already running

    Logger.getLogger(GraphWriter.class).setLevel(Level.DEBUG);
    LOGGER.info("connecting with an existing GraphWriter to shut it down...");
    final boolean isOK = GraphWriter.issuePHPGraphRequest("quit", "quit");
    if (isOK) {
      LOGGER.info("  existing GraphWriter server shut down");
    } else {
      LOGGER.info("  no existing GraphWriter server to shut down");
    }
    LOGGER.info("waiting 5 seconds for possible running GraphWriter to shutdown...");
    Thread.sleep(5_000);
    assertFalse(GraphWriter.isGraphServerRunning());
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    GraphWriter.shutDown();
    try {
      Thread.sleep(2_000); // 2 seconds
    } catch (InterruptedException ex) {
      // ignore
    }
    assertFalse(GraphWriter.isGraphServerRunning());
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of graphPHPSyntaxTree method, of class GraphWriter.
   */
  @Test
  public void testGraphLabeledTree2() {
    LOGGER.info("graphLabeledTree2");

    Logger.getLogger(GraphWriter.class).setLevel(Level.DEBUG);
    LOGGER.info("----------------------------------------------------------------");
    LOGGER.info("asserting that the graph-writing server is not running...");
    assertFalse(GraphWriter.isGraphServerRunning());

    LOGGER.info("----------------------------------------------------------------");
    LOGGER.info("ensuring that the graph-writing server running...");
    GraphWriter.ensureRunningGraphServer();

    LOGGER.info("----------------------------------------------------------------");
    LOGGER.info("asserting that the graph-writing server is running...");
    assertTrue(GraphWriter.isGraphServerRunning());

    LOGGER.info("----------------------------------------------------------------");
    issueGraphRequest(1);
    issueGraphRequest(2);
    issueGraphRequest(3);
    issueGraphRequest(4);
    issueGraphRequest(5);
    issueGraphRequest(6);
    issueGraphRequest(7);
    issueGraphRequest(8);
    issueGraphRequest(9);
    issueGraphRequest(10);
    LOGGER.info("sleeping for 10 seconds while graph images are created by spawned processes...");
    try {
      Thread.sleep(10_000); // 10 seconds
    } catch (InterruptedException ex) {
      // ignore
    }

    final String userHomeDirectory = System.getProperty("user.home");
    assert StringUtils.isNonEmptyString(userHomeDirectory);
    final File file = new File(userHomeDirectory + "/GraphWriter-1.0/log/GraphWriter.log");
    LOGGER.info("contents of: " + file.toString() + "...");

    LOGGER.info("----------------------------------------------------------------");
    try (final Scanner scanner = new Scanner(file)) {
      while (scanner.hasNextLine()) {
        final String line = scanner.nextLine();
        LOGGER.info(line);
      }
    } catch (FileNotFoundException ex) {
      throw new TexaiException(ex);
    }
    LOGGER.info("----------------------------------------------------------------");
  }

  private void issueGraphRequest(final int sequence) {
    final String fileName = System.getProperty("user.home") + "/GraphWriter-1.0/graphs/test";
    String labeledTree = "[test_" + sequence + "/1]";
    GraphWriter.issuePHPGraphRequest(
            fileName + sequence,
            labeledTree);
  }

}
