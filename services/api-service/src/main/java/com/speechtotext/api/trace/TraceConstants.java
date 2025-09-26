package com.speechtotext.api.trace;

/**
 * Constants for request tracing throughout the application.
 */
public final class TraceConstants {
    
    // Headers
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String TRACE_ID_HEADER = "X-Trace-ID";
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String CLIENT_IP_HEADER = "X-Forwarded-For";
    
    // MDC Keys
    public static final String CORRELATION_ID = "correlationId";
    public static final String REQUEST_ID = "requestId";
    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String CLIENT_IP = "clientIp";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String USER_AGENT = "userAgent";
    public static final String JOB_ID = "jobId";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_SIZE = "fileSize";
    public static final String LANGUAGE = "language";
    public static final String MODEL = "model";
    public static final String OPERATION = "operation";
    public static final String DURATION_MS = "durationMs";
    public static final String RESPONSE_STATUS = "responseStatus";
    public static final String RESPONSE_SIZE = "responseSize";
    
    // Trace Operations
    public static final String OP_FILE_UPLOAD = "file_upload";
    public static final String OP_TRANSCRIPTION_CREATE = "transcription_create";
    public static final String OP_TRANSCRIPTION_GET = "transcription_get";
    public static final String OP_TRANSCRIPTION_DOWNLOAD = "transcription_download";
    public static final String OP_TRANSCRIPTION_LIST = "transcription_list";
    public static final String OP_TRANSCRIPTION_DELETE = "transcription_delete";
    public static final String OP_MODEL_SELECTION = "model_selection";
    public static final String OP_S3_UPLOAD = "s3_upload";
    public static final String OP_S3_DOWNLOAD = "s3_download";
    public static final String OP_EXTERNAL_SERVICE_CALL = "external_service_call";
    public static final String OP_DATABASE_OPERATION = "database_operation";
    public static final String OP_CALLBACK_PROCESSING = "callback_processing";
    
    private TraceConstants() {
        throw new IllegalStateException("Utility class");
    }
}
