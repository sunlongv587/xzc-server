package xzc.server.bean;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@ToString
@Accessors(chain = true)
public class UserInfo {
    private long uid;

    private String nickname;

    private String avatar;

    private BigDecimal beans;
}
