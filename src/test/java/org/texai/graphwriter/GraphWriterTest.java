/*
 * GraphWriterTest.java
 *
 * Created on Jun 30, 2008, 8:58:26 AM
 *
 * Description: .
 *
 * Copyright (C) Aug 10, 2011 reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.graphwriter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
    final boolean isOK = GraphWriter.issueGraphRequest("quit", "quit");
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
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of graphLabeledTree method, of class GraphWriter.
   */
  @Test
  public void testGraphLabeledTree2() {
    LOGGER.info("graphLabeledTree2");
    Logger.getLogger(GraphWriter.class).setLevel(Level.DEBUG);
    LOGGER.info("asserting that the graph-writing server is not running...");
    assertFalse(GraphWriter.isGraphServerRunning());
    GraphWriter.ensureRunningGraphServer();
    LOGGER.info("asserting that the graph-writing server is running...");
    assertTrue(GraphWriter.isGraphServerRunning());
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
    GraphWriter.shutDown();
    try {
      Thread.sleep(2_000); // 2 seconds
    } catch (InterruptedException ex) {
      // ignore
    }
    assertFalse(GraphWriter.isGraphServerRunning());
  }

  private void issueGraphRequest(final int sequence) {
    final String fileName = System.getProperty("user.home") + "/GraphWriter-1.0/graphs/test";
    String labeledTree = "[test_" + sequence + "/1]";
    GraphWriter.issueGraphRequest(
            fileName + sequence,
            labeledTree);
  }

}
