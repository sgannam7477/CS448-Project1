/* ... */

package bufmgr;

import java.io.*;
import java.util.*;
import diskmgr.*;
import global.*;

public class BufMgr implements GlobalConst{

  String repPolicy;
  FrameDesc descriptors[];

  Page bufferPool[];

  MyHash hashmap;

  ArrayList<Integer> order;

  int numPages;



  /**
   * Create the BufMgr object.
   * Allocate pages (frames) for the buffer pool in main memory and
   * make the buffer manage aware that the replacement policy is
   * specified by replacerArg.
   *
   * @param numbufs number of buffers in the buffer pool.
   * @param replacerArg name of the buffer replacement policy.
   */

  public BufMgr(int numbufs, String replacerArg) {

    //YOUR CODE HERE

    repPolicy = replacerArg;

    descriptors = new FrameDesc[numbufs];
    bufferPool = new Page[numbufs];

    hashmap = new MyHash();

    order = new ArrayList<>();

    numPages = 0;

    for (int x = 0; x < numbufs; x++) {
      descriptors[x] = new FrameDesc(-1, 0, false);
      bufferPool[x] = new Page();
    }

  }


  /**
   * Pin a page.
   * First check if this page is already in the buffer pool.
   * If it is, increment the pin_count and return a pointer to this
   * page.  If the pin_count was 0 before the call, the page was a
   * replacement candidate, but is no longer a candidate.
   * If the page is not in the pool, choose a frame (from the
   * set of replacement candidates) to hold this page, read the
   * page (using the appropriate method from {diskmgr} package) and pin it.
   * Also, must write out the old page in chosen frame if it is dirty
   * before reading new page.  (You can assume that emptyPage==false for
   * this assignment.)
   *
   * @param pin_pgid page number in the minibase.
   * @param page the pointer poit to the page.
   * @param emptyPage true (empty page); false (non-empty page)
   */

  public void pinPage(PageId pin_pgid, Page page, boolean emptyPage) throws BufferPoolExceededException{

    //YOUR CODE HERE


    numPages = Math.max(numPages, pin_pgid.pid + 1);

    int index = hashmap.get(pin_pgid.pid);
    if (index > -1) {
      descriptors[index].pin_count++;
      //bufferPool[index].setpage(page.getpage());
      page.setpage(bufferPool[index].getpage());
    }
    else {

      for (int x = 0; x < descriptors.length; x++) {
        if (descriptors[x].page_number == -1) {
          index = x;

          try {
            SystemDefs.JavabaseDB.read_page(pin_pgid, bufferPool[index]);
          } catch (InvalidPageNumberException e) {
            throw new RuntimeException(e);
          } catch (FileIOException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          page.setpage(bufferPool[index].getpage());


          descriptors[index].page_number = pin_pgid.pid;
          descriptors[index].pin_count = 0;
          descriptors[index].dirtybit = false;
          order.add(pin_pgid.pid);
          hashmap.add(pin_pgid.pid, index);
          descriptors[index].pin_count++;
          return;
        }
      }
      for (int x = 0; x < order.size(); x++) {
        if (descriptors[hashmap.get(order.get(x))].pin_count == 0) {
          index = hashmap.get(order.get(x));
          break;
        }
      }

      if (index < 0) {
        throw new BufferPoolExceededException(new Exception(), "Ran out of unpinned buffer space");
      }
      else {

        order.remove(Integer.valueOf(descriptors[index].page_number));
        hashmap.remove(descriptors[index].page_number);
        if (descriptors[index].dirtybit) {
          try {
            SystemDefs.JavabaseDB.write_page(new PageId(descriptors[index].page_number), bufferPool[index]);
          } catch (InvalidPageNumberException e) {
            throw new RuntimeException(e);
          } catch (FileIOException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }

        try {
          SystemDefs.JavabaseDB.read_page(pin_pgid, bufferPool[index]);
        } catch (InvalidPageNumberException e) {
          throw new RuntimeException(e);
        } catch (FileIOException e) {
          throw new RuntimeException(e);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        page.setpage(bufferPool[index].getpage());
        descriptors[index].page_number = pin_pgid.pid;
        descriptors[index].pin_count = 0;
        descriptors[index].dirtybit = false;
        order.add(pin_pgid.pid);
        hashmap.add(pin_pgid.pid, index);
        descriptors[index].pin_count++;
      }
    }

  }


  /**
   * Unpin a page specified by a pageId.
   * This method should be called with dirty==true if the client has
   * modified the page.  If so, this call should set the dirty bit
   * for this frame.  Further, if pin_count>0, this method should
   * decrement it. If pin_count=0 before this call, throw an exception
   * to report error.  (For testing purposes, we ask you to throw
   * an exception named PageUnpinnedException in case of error.)
   *
   * @param PageId_in_a_DB page number in the minibase.
   * @param dirty the dirty bit of the frame
   */

  public void unpinPage(PageId PageId_in_a_DB, boolean dirty) throws PageUnpinnedException, HashEntryNotFoundException {

    //pageValues();
    //System.out.println();



    int index = hashmap.get(PageId_in_a_DB.pid);

    //bufferPool[index].setpage(Arrays.copyOf(bufferPool[index].getpage(), bufferPool[index].getpage().length));


    if (index > -1) {
      if (descriptors[index].pin_count == 0) {
        throw new PageUnpinnedException(new Exception(), "Attempting to unpin page without pin");
      }
      else {
        descriptors[index].pin_count--;
        descriptors[index].dirtybit |= dirty;
      }
    }
    else {
      throw new HashEntryNotFoundException(new Exception(), "Attempting to unpin a page not in the buffer frame");
    }
  }


  /**
   * Allocate new pages.
   * Call DB object to allocate a run of new pages and
   * find a frame in the buffer pool for the first page
   * and pin it. (This call allows a client of the Buffer Manager
   * to allocate pages on disk.) If buffer is full, i.e., you
   * can't find a frame for the first page, ask DB to deallocate
   * all these pages, and return null.
   *
   * @param firstpage the address of the first page.
   * @param howmany total number of allocated new pages.
   *
   * @return the first page id of the new pages.  null, if error.
   */

  public PageId newPage(Page firstpage, int howmany) throws BufferPoolExceededException {
      //YOUR CODE HERE

    PageId page_num = new PageId(numPages);
    //System.out.println("A");
    numPages++;

    try {
      SystemDefs.JavabaseDB.allocate_page(page_num, howmany);
    } catch (OutOfSpaceException e) {
      throw new RuntimeException(e);
    } catch (InvalidRunSizeException e) {
      throw new RuntimeException(e);
    } catch (InvalidPageNumberException e) {
      throw new RuntimeException(e);
    } catch (FileIOException e) {
      throw new RuntimeException(e);
    } catch (DiskMgrException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    //System.out.println(page_num);




    pinPage(page_num, firstpage, false);



    return page_num;
  }


  /**
   * This method should be called to delete a page that is on disk.
   * This routine must call the method in diskmgr package to
   * deallocate the page.
   *
   * @param globalPageId the page number in the data base.
   */

  public void freePage(PageId globalPageId) throws InvalidRunSizeException, InvalidPageNumberException, IOException, FileIOException, DiskMgrException, PagePinnedException {
      //YOUR CODE HERE
    int index = hashmap.get(globalPageId.pid);
    if (index > -1) {
      if (descriptors[index].pin_count > 1) {
        throw new PagePinnedException(new Exception(), "Attempting to free pinned page");
      }
      hashmap.remove(globalPageId.pid);
      order.remove(Integer.valueOf(globalPageId.pid));
      descriptors[index].pin_count = 0;
      descriptors[index].page_number = -1;
      descriptors[index].dirtybit = false;
    }
    SystemDefs.JavabaseDB.deallocate_page(globalPageId);


  }


  /**
   * Used to flush a particular page of the buffer pool to disk.
   * This method calls the write_page method of the diskmgr package.
   *
   * @param pageid the page number in the database.
   */

  public void flushPage(int pageid) throws HashEntryNotFoundException {

    //System.out.println("E");

    int index = hashmap.get(pageid);
    if (index < 0 ) {
      throw new HashEntryNotFoundException(new Exception(), "Given pageid doesn't exist");
    }
    try {
      SystemDefs.JavabaseDB.write_page(new PageId(descriptors[index].page_number), bufferPool[index]);
    } catch (InvalidPageNumberException e) {
      throw new RuntimeException(e);
    } catch (FileIOException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    //YOUR CODE HERE

  }

  /** Flushes all pages of the buffer pool to disk
   */

  public void flushAllPages() {
      //YOUR CODE HERE
    //System.out.println(order);
    //System.out.println("F");

    //System.out.println(order);
    for (int x = 0; x < order.size(); x++) {
      try {
        flushPage(order.get(x));
      } catch (HashEntryNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
  }


  /** Gets the total number of buffers.
   *
   * @return total number of buffer frames.
   */

  public int getNumBuffers() {

    //System.out.println("G");


    //YOUR CODE HERE
      return bufferPool.length;
  }


  /** Gets the total number of unpinned buffer frames.
   *
   * @return total number of unpinned buffer frames.
   */

  public int getNumUnpinnedBuffers() {
    //YOUR CODE HERE
    int unpinned = 0;
    for (int x = 0; x < descriptors.length; x++) {
      if (descriptors[x].pin_count == 0) {
        unpinned++;
      }
    }
    return unpinned;
  }
}

