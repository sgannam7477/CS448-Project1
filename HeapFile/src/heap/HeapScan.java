package heap;

import java.util.*;
import global.*;
import chainexception.ChainException;
import diskmgr.*;
import global.*;

/**
 * A HeapScan object is created only through the function openScan() in the
 * HeapFile class. It supports the getNext interface which will simply retrieve
 * the next record in the file.
 */
public class HeapScan implements GlobalConst {

  private HFPage currentPage;
  private RID currentRid;
  private boolean open;

  /**
   * Constructs a file scan by pinning the directoy header page and initializing
   * iterator fields.
   */
  protected HeapScan(HeapFile hf) {
    currentPage = new HFPage();
    Minibase.BufferManager.pinPage(hf.getHead(), currentPage, false);
    currentRid = currentPage.firstRecord();
    open = true;
  }

  /**
   * Called by the garbage collector when there are no more references to the
   * object; closes the scan if it's still open.
   */
  protected void finalize() throws Throwable {
    close();
  }

  /**
   * Closes the file scan, releasing any pinned pages.
   */
  public void close() throws InvalidUpdateException {
    if (open) {
      Minibase.BufferManager.unpinPage(currentPage.getCurPage(), true);
      open = false;
    }
  }

  /**
   * Returns true if there are more records to scan, false otherwise.
   */
  public boolean hasNext() {
    return currentRid == null;
  }

  /**
   * Gets the next record in the file scan.
   * 
   * @param rid output parameter that identifies the returned record
   * @throws IllegalStateException if the scan has no more elements
   */
  public Tuple getNext(RID rid) {
    if (currentRid == null) {
      Minibase.BufferManager.unpinPage(currentPage.getCurPage(), true);
      open = false;
      return null;
    }

    rid.copyRID(currentRid);
    byte[] record = currentPage.selectRecord(rid);
    Tuple currentRecord = new Tuple(record, 0, record.length);

    currentRid = currentPage.nextRecord(currentRid);
    if (currentRid == null) {
      PageId currentPageId = currentPage.getCurPage();
      PageId nextPageId = currentPage.getNextPage();

      if (nextPageId.pid >= 0) {
        Minibase.BufferManager.unpinPage(currentPageId, true);
        Minibase.BufferManager.pinPage(nextPageId, currentPage, false);
        currentRid = currentPage.firstRecord();
      } else {
        currentRid = null;
      }
    }

    return currentRecord;
  }

} // public class HeapScan implements GlobalConst
