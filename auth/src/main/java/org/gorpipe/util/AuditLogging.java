package org.gorpipe.util;

import org.gorpipe.gor.auth.GorAuthInfo;
import org.slf4j.Logger;
import java.util.function.Function;

import static net.logstash.logback.argument.StructuredArguments.value;

public class AuditLogging {

    private static Function<Object[], Object> valueFun = (Object[] args) -> {
        return value((String)args[0], args[1]);
    };

    public static void updateValueFunction(Function<Object[], Object> newValueFun) {
        valueFun = newValueFun;
    }

    private AuditLogging() {
        
    }

    public static void logAudit(Logger auditLog, String message, String context, String action, GorAuthInfo gorAuthInfo) {

        if (gorAuthInfo == null) {
            auditLog.info(message,
                    valueFun.apply(new Object[]{"context", context}),
                    valueFun.apply(new Object[]{"action", action}),
                    valueFun.apply(new Object[]{"auth_status", "UNAUTHENTICATED"}));
        } else {
            auditLog.info(message,
                    valueFun.apply(new Object[]{"context", context}),
                    valueFun.apply(new Object[]{"action", action}),
                    valueFun.apply(new Object[]{"username", gorAuthInfo.getUsername()}),
                    valueFun.apply(new Object[]{"project", gorAuthInfo.getProject()}),
                    valueFun.apply(new Object[]{"sessionId", gorAuthInfo.hashCode()}),
                    valueFun.apply(new Object[]{"auth_status", "OK"})
            );
        }
    }
}
