package org.madgik.rest.enums;

import org.madgik.utils.Constants;

public enum ResponseStatus {
    SUCCESS, FAIL;

    public static String getStatusFromEnum(ResponseStatus status) {
        switch (status) {
            case SUCCESS:
                return Constants.SUCCESS;
            case FAIL:
                return Constants.FAIL;
            default:
                return null;
        }
    }

    public static ResponseStatus getEnumFromString(String status) {
        if (status.equalsIgnoreCase(Constants.SUCCESS)) return SUCCESS;
        if (status.equalsIgnoreCase(Constants.FAIL)) return FAIL;
        return null;
    }

    @Override
    public String toString() {
        return getStatusFromEnum(this);
    }
}
