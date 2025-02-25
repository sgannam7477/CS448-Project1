package heap;

import java.io.*;
import java.util.*;
import diskmgr.*;
import global.*;

/**
 * <h3>Minibase Heap Files</h3>
 * A heap file is an unordered set of records, stored on a set of pages. This
 * class provides basic support for inserting, selecting, updating, and deleting
 * records. Temporary heap files are used for external sorting and in other
 * relational operators. A sequential scan of a heap file (via the Scan class)
 * is the most basic access method.
 */
public class HeapFile implements GlobalConst {

  private String name;
  private PageId head;
  private PageId tail;

  private TreeMap<Short, HashSet<PageId>> freepages;

  /**
   * If the given name already denotes a file, this opens it; otherwise, this
   * creates a new empty file. A null name produces a temporary heap file which
   * requires no DB entry.
   */
  public HeapFile(String name) {
    this.name = name;
    freepages = new TreeMap<Short, HashSet<PageId>>();

    head = Minibase.DiskManager.get_file_entry(name);
    HFPage apage = new HFPage();
    if (head != null) {
      tail = head;
      Minibase.BufferManager.pinPage(tail, apage, false);
      while (apage.getNextPage().pid >= 0) {
        addFreePage(tail);
        PageId tmp = apage.getNextPage();
        Minibase.BufferManager.unpinPage(tail, false);
        tail = tmp;
        Minibase.BufferManager.pinPage(tail, apage, false);
      }
      Minibase.BufferManager.unpinPage(tail, false);
    } else {
      head = Minibase.BufferManager.newPage(apage, 1);
      tail = head;
      apage.setCurPage(head);

      if (name != null) {
        Minibase.DiskManager.add_file_entry(name, head);
      }

      addFreePage(head);
      Minibase.BufferManager.unpinPage(head, true);
    }
  }

  protected void addFreePage(PageId p) {
    HFPage apage = new HFPage();
    Minibase.BufferManager.pinPage(p, apage, false);
    if (apage.getFreeSpace() == 0) {
      Minibase.BufferManager.unpinPage(p, false);
      return;
    }

    HashSet<PageId> set = freepages.get(apage.getFreeSpace());
    if (set == null) {
      set = new HashSet<PageId>();
      freepages.put(apage.getFreeSpace(), set);
    }
    set.add(p);
    Minibase.BufferManager.unpinPage(p, false);
  }

  protected boolean removeFreePage(PageId p) {
    HFPage apage = new HFPage();
    Minibase.BufferManager.pinPage(p, apage, false);
    short size = apage.getFreeSpace();
    Minibase.BufferManager.unpinPage(p, false);

    HashSet<PageId> set = freepages.get(size);
    if (set == null || !set.remove(p)) {
      return false;
    }

    if (set.size() == 0) {
      freepages.remove(size);
    }
    return true;
  }

  protected PageId getFreePage(short size) throws SpaceNotAvailableException {
    if (freepages.higherEntry(size) == null ||
        freepages.higherEntry(size).getValue().size() == 0 ||
        freepages.higherEntry(size).getKey() - size <= 0) {
      HFPage apage = new HFPage();
      PageId p = Minibase.BufferManager.newPage(apage, 1);
      if (apage.getFreeSpace() < size) {
        Minibase.BufferManager.unpinPage(p, true);
        Minibase.BufferManager.freePage(p);
        throw new SpaceNotAvailableException("requested size is bigger than a page");
      }

      apage.setPrevPage(tail);
      apage.setCurPage(p);
      Minibase.BufferManager.pinPage(tail, apage, false);
      apage.setNextPage(p);
      Minibase.BufferManager.unpinPage(tail, true);
      tail = p;

      addFreePage(p);
      Minibase.BufferManager.unpinPage(p, true);
    }
    return freepages.higherEntry(size).getValue().stream().findFirst().get();
  }

  /**
   * Called by the garbage collector when there are no more references to the
   * object; deletes the heap file if it's temporary.
   */
  protected void finalize() throws Throwable {
    if (name.equals(null)) {
      deleteFile();
    }
  }

  /**
   * Deletes the heap file from the database, freeing all of its pages.
   */
  public void deleteFile() {
    PageId p = head;
    HFPage apage = new HFPage();

    Minibase.BufferManager.pinPage(p, apage, false);
    while (apage.getNextPage().pid >= 0) {
      PageId tmp = apage.getNextPage();
      Minibase.BufferManager.unpinPage(p, false);
      Minibase.BufferManager.freePage(p);
      p = tmp;
      Minibase.BufferManager.pinPage(p, apage, false);
    }
    Minibase.BufferManager.unpinPage(p, false);
  }

  /**
   * Inserts a new record into the file and returns its RID.
   * 
   * @throws IllegalArgumentException if the record is too large
   */
  public RID insertRecord(byte[] record) throws SpaceNotAvailableException {
    PageId p = getFreePage((short) (record.length + 1));
    HFPage apage = new HFPage();

    Minibase.BufferManager.pinPage(p, apage, false);
    removeFreePage(p);
    RID rid = apage.insertRecord(record);
    addFreePage(p);
    Minibase.BufferManager.unpinPage(p, true);

    return rid;
  }

  /**
   * Reads a record from the file, given its id.
   * 
   * @throws IllegalArgumentException if the rid is invalid
   */
  public Tuple getRecord(RID rid) {
    PageId p = head;
    HFPage apage = new HFPage();

    Minibase.BufferManager.pinPage(p, apage, false);
    while (apage.getNextPage().pid >= 0) {
      if (apage.getCurPage().equals(rid.pageno)) {
        byte[] record = apage.selectRecord(rid);
        return new Tuple(record, 0, record.length);
      }

      PageId tmp = apage.getNextPage();
      Minibase.BufferManager.unpinPage(p, false);
      p = tmp;
      Minibase.BufferManager.pinPage(p, apage, false);
    }
    if (apage.getCurPage().equals(rid.pageno)) {
      byte[] record = apage.selectRecord(rid);
      return new Tuple(record, 0, record.length);
    }
    Minibase.BufferManager.unpinPage(p, false);

    throw new IllegalArgumentException("RID pageno not in HeapFile");
  }

  /**
   * Updates the specified record in the heap file.
   * 
   * @throws IllegalArgumentException if the rid or new record is invalid
   */
  public boolean updateRecord(RID rid, Tuple newRecord) throws InvalidUpdateException {
    HFPage apage = new HFPage();

    Minibase.BufferManager.pinPage(rid.pageno, apage, false);
    apage.updateRecord(rid, newRecord);
    Minibase.BufferManager.unpinPage(rid.pageno, true);

    return true;
  }

  /**
   * Deletes the specified record from the heap file.
   * 
   * @throws IllegalArgumentException if the rid is invalid
   */
  public boolean deleteRecord(RID rid) {
    HFPage apage = new HFPage();
    Minibase.BufferManager.pinPage(rid.pageno, apage, false);
    apage.deleteRecord(rid);
    Minibase.BufferManager.unpinPage(rid.pageno, true);

    return true;
  }

  /**
   * Gets the number of records in the file.
   */
  public int getRecCnt() {
    int recCnt = 0;

    PageId p = head;
    HFPage apage = new HFPage();

    Minibase.BufferManager.pinPage(p, apage, false);
    while (apage.getNextPage().pid >= 0) {
      recCnt += apage.getSlotCount();
      PageId tmp = apage.getNextPage();
      Minibase.BufferManager.unpinPage(p, false);
      p = tmp;
      Minibase.BufferManager.pinPage(p, apage, false);
    }
    recCnt += apage.getSlotCount();
    Minibase.BufferManager.unpinPage(p, false);

    return recCnt;
  }

  /**
   * Initiates a sequential scan of the heap file.
   */
  public HeapScan openScan() {
    return new HeapScan(this);
  }

  /**
   * Returns the name of the heap file.
   */
  public String toString() {
    return name;
  }

  public PageId getHead() {
    return head;
  }
} // public class HeapFile implements GlobalConst
