import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class SynchronizedArrayList implements StringList {
    private final List<String> list = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addFirst(String str) {
        synchronized (list) {
            list.add(0, str);
        }
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean sortStep(int position, long delayInsideStep) {
        synchronized (list) {
            int index = position;

            if (index < 0 || index + 1 >= list.size()) return false;

            String first = list.get(index);
            String second = list.get(index + 1);

            try {
                if (delayInsideStep > 0) {
                    Thread.sleep(delayInsideStep);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            if (first.compareTo(second) > 0) {
                list.set(index, second);
                list.set(index + 1, first);
            }

            return true;
        }
    }

    @Override
    public Iterator<String> iterator() {
        List<String> iter = new ArrayList<>();

        synchronized (list) {
            iter.addAll(list);
        }

        return iter.iterator();
    }
}
