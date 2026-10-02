package topup;

import lombok.Data;

import java.time.LocalDate;

@Data
public class Topup {
    private int id;
    private String name;
    private long amount;
    private TopupType type;
    private Status status;
    private LocalDate date;
}
