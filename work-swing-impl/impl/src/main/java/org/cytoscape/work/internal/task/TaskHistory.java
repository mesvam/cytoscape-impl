package org.cytoscape.work.internal.task;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.cytoscape.work.FinishStatus;
import org.cytoscape.work.TaskMonitor;

/*
 * #%L
 * Cytoscape Work Swing Impl (work-swing-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2021 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

/**
 * A data structure for representing histories of task iterators.
 * The {@code TaskHistory} instance can consist of many {@code History} instances,
 * which represents the history of a single task iterator execution.
 * {@code History}'s contain the task's title, completion status, and {@code Message}s.
 *
 * <p>
 * This class is thread-safe.
 * </p>
 */
public class TaskHistory implements Iterable<Object> {
	
  public static interface FinishListener {
    public void taskFinished(History history);
  }

  public static final TaskMonitor.Level[] levels = TaskMonitor.Level.values();
  public static final FinishStatus.Type[] finishTypes = FinishStatus.Type.values();

  public static class Message {
    // Use a byte to reference TaskMonitor.Level to save memory
    final byte levelOrdinal;
    final String message;

    public Message(final TaskMonitor.Level level, final String message) {
      this.levelOrdinal = (level == null) ? -1 : (byte) level.ordinal();
      this.message = message;
    }

    public TaskMonitor.Level level() {
      return (this.levelOrdinal < 0) ? null : levels[this.levelOrdinal];
    }

    public String message() {
      return message;
    }
  }

  public class History implements Iterable<Message> {
	
    /** Use a byte to reference FinishStatus.Type to save memory */
    volatile byte finishType = -1;
    volatile Class<?> firstTaskClass;
    final AtomicReference<String> title = new AtomicReference<>();
    final ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<>();

    protected History() {}

    public void setTitle(final String newTitle) {
      if (!title.compareAndSet(null, newTitle)) {
        addMessage(null, newTitle);
      }
    }

    public void setFinishType(final FinishStatus.Type finishType) {
      this.finishType = (finishType == null) ? -1 : (byte) finishType.ordinal();
      if (finishListener != null) {
        finishListener.taskFinished(this);
      }
    }

    public void addMessage(final TaskMonitor.Level level, final String message) {
      messages.add(new Message(level, message));
    }

    @Override
    public Iterator<Message> iterator() {
      return messages.iterator();
    }

    public String getTitle() {
      return title.get();
    }

    public FinishStatus.Type getFinishType() {
      return finishType < 0 ? null : finishTypes[finishType];
    }

    public boolean hasMessages() {
      return !messages.isEmpty();
    }

    public void setFirstTaskClass(final Class<?> klass) {
      this.firstTaskClass = klass;
    }

    public Class<?> getFirstTaskClass() {
      return firstTaskClass;
    }
  }

  final ConcurrentLinkedQueue<Object> histories = new ConcurrentLinkedQueue<>();
  volatile FinishListener finishListener;

  /**
   * Create a new {@code History} for a single task.
   */
  public History newHistory() {
    final History history = new History();
    histories.add(history);
    return history;
  }

  /**
   * Return all {@code History}'s contained in this instance.
   */
  @Override
  public Iterator<Object> iterator() {
    return histories.iterator();
  }

  /**
   * Clear out all {@code History}'s contained in this instance.
   */
  public void clear() {
    histories.clear();
  }

  public void setFinishListener(final FinishListener finishListener) {
    this.finishListener = finishListener;
  }

  public void addUnnestedMessage(final TaskMonitor.Level level, final String message) {
    histories.add(new Message(level, message));
  }
}