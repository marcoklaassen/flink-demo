package click.klaassen.flink;

public class MyEvent {
    private String user;
    private int counter;

    public MyEvent() {}
    
    public MyEvent(String user, int counter) {
        this.user = user;
        this.counter = counter;
    }

    public String getUser() { 
        return user; 
    }
    
    public int getCounter() { 
        return counter; 
    }

    @Override
    public String toString() {
        return "User: " + user + " / counter: " + counter; 
    }
}