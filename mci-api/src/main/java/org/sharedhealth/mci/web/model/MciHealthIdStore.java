package org.sharedhealth.mci.web.model;

import org.sharedhealth.mci.web.exception.HealthIdExhaustedException;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MciHealthIdStore {
    private static MciHealthIdStore mciHealthIdStore;

    private Queue<String> mciHealthIds;

    public static MciHealthIdStore getInstance() {
        if (mciHealthIdStore != null) return mciHealthIdStore;
        mciHealthIdStore = new MciHealthIdStore();
        return mciHealthIdStore;
    }

    private MciHealthIdStore() {
        this.mciHealthIds = new ConcurrentLinkedQueue<>();
    }

    private Queue<String> getMciHealthIds() {
        return mciHealthIds;
    }

    public synchronized void addMciHealthIds(Collection<String> mciHealthIds) {
        getInstance().getMciHealthIds().addAll(mciHealthIds);
    }

    public synchronized Collection<String> getAll() {
        return Collections.unmodifiableCollection(getInstance().getMciHealthIds());
    }

    public synchronized String getNextHealthId() {
        try {
            return getInstance().getMciHealthIds().remove();
        } catch (Exception e) {
            throw new HealthIdExhaustedException();
        }
    }

    public synchronized void clear() {
        getInstance().getMciHealthIds().clear();
    }

    public int noOfHIDsLeft() {
        return getInstance().getMciHealthIds().size();
    }
}
