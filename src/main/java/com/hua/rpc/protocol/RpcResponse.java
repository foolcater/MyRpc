package com.hua.rpc.protocol;

/**
 * RPC response
 * @author hua
 */
public class RpcResponse {

    private String requestId;
    private String error;
    private Object object;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public boolean isError() {
        return error != null;
    }
}
