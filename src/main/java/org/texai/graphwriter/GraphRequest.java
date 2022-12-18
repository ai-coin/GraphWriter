/*
 * GraphRequest.java
 *
 * Created on Aug 10, 2011, 8:33:33 AM
 *
 * Description: Provides a PHP syntax tree graph request.
 *
 * Copyright (C) Aug 10, 2011, Stephen L. Reed, Texai.org.
 *
 */
package org.texai.graphwriter;

import java.io.BufferedReader;
import java.io.IOException;
import org.texai.util.StringUtils;

/**
 * Provides a graph request.
 *
 * @author reed
 */
public class GraphRequest {

  // the serial version UID
  private static final long serialVersionUID = 1L;

  // the graph file name without an extension
  private String fileName;

  // the labeled tree that specifies the PHP syntax graph
  private String labeledTree;

  /**
   * Constructs a new GraphRequest instance.
   *
   * @param fileName the graph file name
   * @param labeledTree the labeled tree that specifies the graph
   */
  public GraphRequest(
          final String fileName,
          final String labeledTree) {
    //Preconditions
    assert labeledTree != null : "labeledTree must not be null";
    assert !labeledTree.isEmpty() : "labeledTree must not be empty";

    this.fileName = fileName;
    this.labeledTree = labeledTree;
  }

  /**
   * Makes a GraphRequest instance from the contents of the given buffered reader, as received from the client.
   *
   * @param bufferedReader the given buffered reader
   * @return a GraphRequest instance
   */
  public static GraphRequest makeGraphRequest(final BufferedReader bufferedReader) {
    //Preconditions
    assert bufferedReader != null : "bufferedReader must not be null";

    final StringBuilder stringBuilder = new StringBuilder();
    try {
      OUTER:
      while (true) {
        final int ch = bufferedReader.read();
        switch (ch) {
          case 0:
            break OUTER;

          case -1:
            throw new RuntimeException("ill formed file name: " + stringBuilder.toString());

          default:
            stringBuilder.append((char) ch);
            break;
        }
      }
      final String fileName = stringBuilder.toString();
      if (fileName.isEmpty()) {
        throw new RuntimeException("graph request is missing the file name");
      }
      stringBuilder.setLength(0);
      OUTER_1:
      while (true) {
        final int ch = bufferedReader.read();
        switch (ch) {
          case 0:
            break OUTER_1;

          case -1:
            throw new RuntimeException("ill formed labeled tree, fileName: " + fileName + ", labeledTree: " + stringBuilder.toString());

          default:
            stringBuilder.append((char) ch);
            break;
        }
      }
      final String labeledTree = stringBuilder.toString();
      if (labeledTree.isEmpty()) {
        throw new RuntimeException("graph request is missing the labeled tree");
      }
      return new GraphRequest(fileName, labeledTree);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Serializes this graph request, for sending to the graph server.
   *
   * @return the serialized request
   */
  public String serialize() {
    final StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(fileName);
    stringBuilder.append((char) 0);
    stringBuilder.append(labeledTree);
    stringBuilder.append((char) 0);
    return stringBuilder.toString();
  }

  /**
   * Gets the graph file name without an extension.
   *
   * @return the graph file name without an extension
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Sets the graph file name without an extension.
   *
   * @param fileName the graph file name without an extension
   */
  public void setFileName(final String fileName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(fileName) : "fileName must be a non-empty character string";

    this.fileName = fileName;
  }

  /**
   * Gets the labeled tree that specifies the graph.
   *
   * @return the labeled tree that specifies the graph
   */
  public String getLabeledTree() {
    return labeledTree;
  }

  /**
   * Returns a string representation of this object.
   *
   * @return a string representation of this object
   */
  @Override
  public String toString() {
    return "[GraphRequest " + fileName + "]";
  }

  /**
   * Sets the labeled tree that specifies the graph.
   *
   * @param labeledTree the labeled tree that specifies the graph
   */
  public void setLabeledTree(final String labeledTree) {
    //Preconditions
    assert labeledTree != null : "labeledTree must not be null";
    assert !labeledTree.isEmpty() : "labeledTree must not be empty";

    this.labeledTree = labeledTree;
  }
}
