import javafx.util.Pair;

public class MaxHeap {
    private Pair<Integer, Double>[] heap;
    private int size;
    private int maxSize;

    public MaxHeap(int maxSize) {
        this.maxSize = maxSize;
        this.size = 0;
        heap = new Pair[this.maxSize + 1];
        heap[0] = new Pair<>(-1, Double.MAX_VALUE);
    }

    private int parent(int pos) {
        return pos / 2;
    }

    private int leftChild(int pos) {
        return (2 * pos);
    }

    private int rightChild(int pos) {
        return (2 * pos) + 1;
    }

    private boolean isLeaf(int pos) {
        return pos > (size / 2.0) && pos <= size;
    }

    private void swap(int fpos, int spos) {
        Pair<Integer, Double> tmp = new Pair<>(heap[fpos].getKey(), heap[fpos].getValue());
        heap[fpos] = new Pair<>(heap[spos].getKey(), heap[spos].getValue());
        heap[spos] = new Pair<>(tmp.getKey(), tmp.getValue());
    }

    private void maxHeapify(int pos) {
        if (isLeaf(pos)) {
            return;
        }
        if (heap[rightChild(pos)] == null) {
            if (heap[pos].getValue() < heap[leftChild(pos)].getValue()) {
                swap(pos, leftChild(pos));
            }
            return;
        }
        if (heap[pos].getValue() < heap[leftChild(pos)].getValue() ||
                heap[pos].getValue() < heap[rightChild(pos)].getValue()) {

            if (heap[leftChild(pos)].getValue() > heap[rightChild(pos)].getValue()) {
                swap(pos, leftChild(pos));
                maxHeapify(leftChild(pos));
            } else {
                swap(pos, rightChild(pos));
                maxHeapify(rightChild(pos));
            }
        }
    }

    public void insert(Pair<Integer, Double> element) {
        heap[++size] = element;

        int current = size;
        while (heap[current].getValue() > heap[parent(current)].getValue()) {
            swap(current, parent(current));
            current = parent(current);
        }
    }

    public Pair<Integer, Double> extractMax() {
        Pair<Integer, Double> popped = heap[1];
        heap[1] = heap[size];
        heap[size] = null;
        size--;
        if (size != 0)
            maxHeapify(1);
        return popped;
    }
}