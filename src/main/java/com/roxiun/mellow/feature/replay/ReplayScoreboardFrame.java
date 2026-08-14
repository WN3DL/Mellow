package com.roxiun.mellow.feature.replay;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReplayScoreboardFrame {

    private int timestampMs;
    private String title;
    private List<String> lines;

    public ReplayScoreboardFrame() {}

    public ReplayScoreboardFrame(int timestampMs, String title, List<String> lines) {
        this.timestampMs = timestampMs;
        this.title = title == null ? "" : title;
        this.lines = lines == null
            ? Collections.<String>emptyList()
            : new ArrayList<>(lines);
    }

    public int getTimestampMs() {
        return timestampMs;
    }

    public String getTitle() {
        return title == null ? "" : title;
    }

    public List<String> getLines() {
        return lines == null ? Collections.<String>emptyList() : lines;
    }
}
