package core.framework.impl.web.management;

import core.framework.impl.web.http.IPAccessControl;
import core.framework.web.Controller;
import core.framework.web.Request;
import core.framework.web.Response;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

/**
 * @author neo
 */
public class MemoryUsageController implements Controller {
    private final IPAccessControl accessControl;

    public MemoryUsageController(IPAccessControl accessControl) {
        this.accessControl = accessControl;
    }

    @Override
    public Response execute(Request request) {
        accessControl.validateClientIP(request.clientIP());
        return Response.bean(memoryUsage());
    }

    private MemoryUsage memoryUsage() {
        MemoryUsage usage = new MemoryUsage();
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        java.lang.management.MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        usage.heapInit = heapMemoryUsage.getInit();
        usage.heapUsed = heapMemoryUsage.getUsed();
        usage.heapCommitted = heapMemoryUsage.getCommitted();
        usage.heapMax = heapMemoryUsage.getMax();

        java.lang.management.MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        usage.nonHeapInit = nonHeapMemoryUsage.getInit();
        usage.nonHeapUsed = nonHeapMemoryUsage.getUsed();
        usage.nonHeapCommitted = nonHeapMemoryUsage.getCommitted();
        usage.nonHeapMax = nonHeapMemoryUsage.getMax();

        return usage;
    }
}
