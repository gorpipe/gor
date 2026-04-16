
# Retry Logic in GOR/GOR Services

GOR/GOR Services supports retry logic at multiple levels.

- Low level retry at the library level.  Some operation, including S3 operation, are retried by the relevant library.
- Retry at the GOR driver level.  The GOR driver supports retry at the GOR driver operations level.  The operation is retried if the a GORRetryException is thrown and the retry flag is set.
- Retry at the GOR subqury level.  The GOR driver supports retry at the GOR sub-query level.  The sub-query is retried if the a GORRetryException is thrown and the fullretry flag is set. The sub-query level retried is needed as not all operations can be retried at the operation level, for example many errors when writing/reading to/from a stream.

## Lov level retry

Currently known low level retires:

- S3 operations are retried by the AWS SDK.  


## Retry at the GOR driver level

GOR driver level retries ar controlled by the follwoing components:

- RetriableSourceWrapper - this is a wrapper around the source that retries the operation if a GORRetryException is thrown and the retry flag is set.
- RetryHandler - this is a handler taht is used by the RetriableSourceWrapper to retry the operation if a GORRetryException is thrown and the retry flag is set.  We currently have two basic implementations of the RetryHandler, which the retryhandler for each source can extend:
    - RetryHandlerWithFixedRetries - this is a retry handler that retries the operation a fixed number of times. It is used by the RetriableSourceWrapper to retry the operation if a GORRetryException is thrown and the retry flag is set.
      - checkIfShouldRetryException
    - RetryHandlerWithFixedWait - this is a retry handler that retries the operation with a fixed wait time between retries. It is used by the RetriableSourceWrapper to retry the operation if a GORRetryException is thrown and the retry flag is set.
      - checkIfShouldRetryException
- GORRetryException - this is the exception that is thrown when a retry is needed. It has a retry flag that indicates if the operation should be retried or not. If the retry flag is set, the operation will be retried. If the fullretry flag is set, the sub-query will be retried.

For an exception to be retried the following conditions must be met:
1. The source must be wrapped in a RetriableSourceWrapper.
2. The operations in the source must wrap the exception in a GORRetryException with the retry flag set to true.
3. The RetryHandler.checkIfShouldRetryException must not throw an exception for the GORRetryException.
4. The retry count/time must be less than the maximum retry count/time set in the RetryHandler.

## Retry at the GOR sub-query level

GOR sub-query level retries are controlled by the following components:

- GorRetryException - this is the exception that is thrown when a retry is needed at the sub-query level. It has a fullretry flag that indicates if the sub-query should be retried or not. If the fullretry flag is set, the sub-query will be retried.
