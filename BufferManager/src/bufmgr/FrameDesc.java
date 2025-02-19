package bufmgr;
import global.*;


public class FrameDesc {
    protected int page_number;
    protected int pin_count;
    protected boolean dirtybit;

    public FrameDesc(int pn, int pc, boolean db) {
        page_number = pn;
        pin_count = pc;
        dirtybit = db;
    }

    @Override
    public String toString() {
        return Integer.toString(page_number);
    }
}
