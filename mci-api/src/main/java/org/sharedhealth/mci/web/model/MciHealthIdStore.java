package org.sharedhealth.mci.web.model;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MciHealthIdStore {
    private Queue<String> mciHealthIds;

    public void addMciHealthIds(Collection<String> mciHealthIds) {
        if (this.mciHealthIds == null) this.mciHealthIds = new ConcurrentLinkedQueue<>();
        this.mciHealthIds.addAll(mciHealthIds);
    }

    public String getMciHealthId() {
        return this.mciHealthIds.remove();
    }

    public int noOfHidsLeft() {
        return this.mciHealthIds.size();
    }
}
