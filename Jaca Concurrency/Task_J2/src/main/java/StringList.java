package main.java;

public interface StringList extends Iterable<String> {

    void addFirst(String str);

    int size();

    boolean sortStep(int position, long delayInsideStep);

    default void printList() {
        for (String s : this) {
            System.out.println(s);
        }
    }

    default boolean isEmpty() {
        return size() == 0;
    }
}
