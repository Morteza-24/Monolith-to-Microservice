package club.newtech.qbike.order.domain.core.vo;

import lombok.Data;
import lombok.ToString;



@ToString
@Data
public class IntentionVo {
    private int customerId;
    private double startLong;
    private double startLat;
    private double destLong;
    private double destLat;
    private int intentionId;
    private int driverId;


}
