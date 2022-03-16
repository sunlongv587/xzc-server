package xzc.server.exception;

import lombok.Getter;
import xzc.server.proto.common.ErrorCode;

import java.text.MessageFormat;
import java.util.Map;

public class XzcException extends RuntimeException {

    @Getter
    private ErrorCode errorCode;

    @Getter
    private Map<String, String> data;

    private Object[] objects;

    public XzcException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public XzcException(ErrorCode errorCode, String message, Map<String, String> data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    public XzcException(ErrorCode errorCode, String message, Map<String, String> data, Object... objects) {
        this(errorCode, message);
        this.data = data;
        this.objects = objects;
    }

    @Override
    public String getMessage() {
        return MessageFormat.format(super.getMessage(), objects);
    }

    public int getCode() {
        return this.errorCode.getNumber();
    }

}
