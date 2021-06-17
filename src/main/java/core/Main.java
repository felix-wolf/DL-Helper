package core;

public class Main {

    /**
     * main method, point of entry
     * initializes LogFileReader
     * @param args argument with what the application is started with
     */
    public static void main(String[] args) {
        System.out.println("Program started via main method");
        new LogFileReader();
    }

}
