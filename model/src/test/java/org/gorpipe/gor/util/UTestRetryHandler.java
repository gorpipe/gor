package org.gorpipe.gor.util;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.junit.Assert;
import org.junit.Test;

public class UTestRetryHandler {

    @Test
    public void TestRetryHandlerCount() throws Throwable {
        var handler = new TestRetryHandler(100, 1000);
        Assert.assertThrows(GorSystemException.class, () -> {
                    handler.perform(() -> {
                                throw new GorResourceException("Test", "").retry();
                            });
                });

        Assert.assertTrue(handler.getCounter() > 2);
    }

    @Test
    public void TestRetryHandlerWithInvalidDurations() throws Throwable {
        var handler = new TestRetryHandler(1000, 100);
        Assert.assertThrows(AssertionError.class, () -> {
                    handler.perform(() -> {
                                throw new GorResourceException("Test", "").retry();
                            });
                });
    }

    @Test
    public void TestRetryHandlerWithFailureThenSuccess() throws Throwable {
        var handler = new TestRetryHandler(100, 1000);
        var result = handler.perform(() -> {
                    if (handler.getCounter() == 0) {
                        throw new GorResourceException("Test", "").retry();
                    }
                    return "Success";
                });
        Assert.assertEquals("Success", result);
    }

}
