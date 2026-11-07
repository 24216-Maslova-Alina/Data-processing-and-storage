import java.util.Scanner;

public class InputThread extends Thread{
    private final StringList mylist;
    private Scanner scanner;

    public InputThread(StringList mylist, Scanner scanner) {
        this.mylist = mylist;
        this.scanner = scanner;
    }

    @Override
    public void run() {
        System.out.println("Вводите строки. Пустая строка - вывод списка");
        System.out.println("Чтобы завершить ввод и начать сортировку введите 'exit'");

        while (scanner.hasNextLine()) {
            String input = scanner.nextLine();

            if (input.equals("exit")){
                break;
            } else if (input.isEmpty()) {
                System.out.println("Текущий список: ");
                mylist.printList();
            } else {
                mylist.addFirst(input);
            }
        }
    }
}
