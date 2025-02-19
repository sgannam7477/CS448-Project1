package bufmgr;

import global.PageId;

import java.util.ArrayList;

public class Bucket {

    ArrayList<Integer> pageNumber;
    ArrayList<Integer> frameNumber;

    public Bucket() {
        pageNumber = new ArrayList<>();
        frameNumber = new ArrayList<>();
    }

    public int find(int pn) {
        int index = pageNumber.indexOf(pn);
        if (index > -1) {
            return frameNumber.get(index);
        }
        else {
            return -1;
        }
    }

    public void add(int fd, int pn) {
        pageNumber.add(pn);
        frameNumber.add(fd);
    }

    public void remove(int pn) {
        int index = pageNumber.indexOf(pn);
        pageNumber.remove(index);
        frameNumber.remove(index);

    }


}
