package core.framework.impl.web;

import core.framework.api.http.HTTPStatus;
import core.framework.impl.web.service.ErrorResponse;
import core.framework.impl.web.service.WebServiceClient;
import core.framework.impl.web.site.AJAXErrorResponse;
import core.framework.log.Severity;
import core.framework.web.exception.NotFoundException;
import core.framework.web.service.RemoteServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class HTTPServerErrorHandlerTest {
    private HTTPServerErrorHandler handler;

    @BeforeEach
    void createHTTPServerErrorHandler() {
        handler = new HTTPServerErrorHandler(null);
    }

    @Test
    void httpStatus() {
        assertThat(handler.httpStatus(new RemoteServiceException("error", Severity.WARN, "error_code", HTTPStatus.BAD_REQUEST)))
                .isEqualTo(HTTPStatus.BAD_REQUEST);

        assertThat(handler.httpStatus(new NotFoundException("error"))).isEqualTo(HTTPStatus.NOT_FOUND);

        assertThat(handler.httpStatus(new Error())).isEqualTo(HTTPStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void errorResponseWithErrorCodeException() {
        var expected = new ErrorResponse();
        expected.id = "actionId";
        expected.message = "test message";
        expected.errorCode = "TEST_ERROR_CODE";
        expected.severity = "WARN";

        var response = handler.errorResponse(new NotFoundException("test message", "TEST_ERROR_CODE"), WebServiceClient.USER_AGENT, "actionId");
        assertThat(response).isInstanceOf(ErrorResponse.class).isEqualToIgnoringGivenFields(expected, "stackTrace");
    }

    @Test
    void errorResponse() {
        var expected = new ErrorResponse();
        expected.id = "actionId";
        expected.message = "test message";
        expected.errorCode = "INTERNAL_ERROR";
        expected.severity = "ERROR";

        var response = handler.errorResponse(new Error("test message"), WebServiceClient.USER_AGENT, "actionId");
        assertThat(response).isInstanceOf(ErrorResponse.class).isEqualToIgnoringGivenFields(expected, "stackTrace");
    }

    @Test
    void ajaxErrorResponse() {
        var expected = new AJAXErrorResponse();
        expected.id = "actionId";
        expected.message = "test message";
        expected.errorCode = "INTERNAL_ERROR";

        var response = handler.errorResponse(new Error("test message"), "Mozilla/5.0", "actionId");
        assertThat(response).isInstanceOf(AJAXErrorResponse.class).isEqualToIgnoringGivenFields(expected, "stackTrace");
    }
}
