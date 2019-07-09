package org.wingsofcarolina.quiz.domain;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Used to store counters for other entities.
 */

@Entity(value = "IDs", noClassnameStored = true)
public class AutoIncrement {

  @Id
  protected String key;

  protected long value = 1L;

  protected AutoIncrement() {
    super();
  }

  /**
   * Set the key name — class or class with some other attribute(s).
   */
  public AutoIncrement(final String key) {
    this.key = key;
  }

  /**
   * Set the key name and initialize the value so it won't start at 1.
   */
  public AutoIncrement(final String key, final long startValue) {
    this(key);
    value = startValue;
  }

  public void setValue(Long value) {
	  this.value = value;
  }
  
  public Long getValue() {
    return value;
  }
}