/**
 * AbstractGraphRequest.java
 *
 * Created on Dec 17, 2022.
 *
 * Description: Provides an abstract graph request
 *
 * Copyright (C) Dec 17, 2022 by Stephen L. Reed.
 */
package org.texai.graphwriter;

import org.apache.log4j.Logger;
import org.texai.util.StringUtils;

public abstract class AbstractGraphRequest {
  // the logger
  private static final Logger LOGGER = Logger.getLogger(AbstractGraphRequest.class);

  // the serial version UID
  private static final long serialVersionUID = 1L;

  // the graph file name
  private String fileName;

  /** 
   * Constructs a new AbstractGraphRequest instance.
   * @param fileName the graph file name
   */
  public AbstractGraphRequest(final String fileName) {
    //Preconditions
    assert StringUtils.isNonEmptyString(fileName) : "fileName must be a non-empty character string";
    
    this.fileName = fileName;
  }

  /**
   * Gets the graph file name.
   *
   * @return the graph file name
   */
  public String getFileName() {
    return fileName;
  }

  /**
   * Sets the graph file name.
   *
   * @param fileName the graph file name
   */
  public void setFileName(final String fileName) {
    //Preconditions
    assert fileName != null : "fileName must not be null";
    assert !fileName.isEmpty() : "fileName must not be empty";

    this.fileName = fileName;
  }
}
