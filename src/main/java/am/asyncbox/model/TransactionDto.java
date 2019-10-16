package am.asyncbox.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TransactionDto {
    long id;
    String description;
    String fromAccount;
    String toAccount;
    long amount;
    int currency;
}
