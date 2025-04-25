package click.klaassen.flink;

public class AggregatedEvent {
    public String user;
    public int total;

    public AggregatedEvent() {}

    public AggregatedEvent(String user, int total) {
        this.user = user;
        this.total = total;
    }

    @Override
    public String toString() {
        return "User: " + user + " / counter: " + total; 
    }
}