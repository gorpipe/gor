package org.gorpipe.data;

public record Segment(int start, int end) {
    public int length() {
        return end - start;
    }

    public int distance(Segment other) {
        if (start >= other.end) {
            return start - other.end;
        }
        if (end <= other.start) {
            return other.start - end;
        }
        return 0;
    }

    public boolean contains(int pos) {
        return pos >= start && pos < end;
    }

    public boolean contains(Segment other) {
        return other.start >= start && other.end <= end;
    }

    public boolean intersects(Segment other) {
        return start < other.end && end > other.start;
    }

    public Segment intersection(Segment other) {
        return new Segment(Math.max(start, other.start), Math.min(end, other.end));
    }

    public Segment union(Segment other) {
        return new Segment(Math.min(start, other.start), Math.max(end, other.end));
    }

    public Segment extend(int length) {
        return new Segment(start, end + length);
    }

    public Segment extend(int length, int max) {
        return new Segment(start, Math.min(end + length, max));
    }
}
