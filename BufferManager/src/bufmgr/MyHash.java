package bufmgr;


import global.PageId;

public class MyHash {

    Bucket directory[];

    private final int HTSIZE = 23;



    public MyHash() {
        directory = new Bucket[HTSIZE];
        for (int x = 0; x < HTSIZE; x++) {
            directory[x] = new Bucket();
        }

    }

    public void add(int pn, int frame) {

        //System.out.println("Add: " + pn);

        int index = hash(pn);
        if (directory[index].find(pn) != -1) {
            directory[index].remove(pn);
        }
        directory[index].add(frame, pn);

    }

    public int get(int pn) {
        //System.out.println("Get: " + pn);
        int index = hash(pn);
        //System.out.println(index);
        return directory[index].find(pn);
    }

    public void remove(int pn) {
        int index = hash(pn);
        directory[index].remove(pn);
    }

    private int hash(int pn) {
        //System.out.println(pn.hashCode());
        return ((23 * pn + 37) % HTSIZE);
    }
}
