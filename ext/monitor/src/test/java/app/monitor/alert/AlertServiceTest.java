package app.monitor.alert;

import app.monitor.AlertConfig;
import app.monitor.slack.SlackClient;
import core.framework.log.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author ericchung
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {
    private AlertService service;
    @Mock
    private SlackClient slackClient;

    @BeforeEach
    void createActionAlertService() {
        var config = new AlertConfig();
        config.ignoreWarnings = List.of(matcher(List.of("website"), List.of("PATH_NOT_FOUND")));
        config.criticalErrors = List.of(matcher(List.of(), List.of("CRITICAL_ERROR", "SLOW_SQL")));
        config.site = "site";
        config.timespanInHours = 4;
        config.kibanaURL = "http://kibana:5601";

        var actionErrorMatcher = new AlertConfig.Matcher();
        actionErrorMatcher.severity = Severity.ERROR;
        actionErrorMatcher.kibanaIndex = "trace";

        var productErrorMatcher = new AlertConfig.Matcher();
        productErrorMatcher.apps = List.of("website");
        productErrorMatcher.errorCodes = List.of("PRODUCT_ERROR");

        var eventErrorMatcher = new AlertConfig.Matcher();
        eventErrorMatcher.severity = Severity.ERROR;
        eventErrorMatcher.kibanaIndex = "event";

        config.channels = Map.ofEntries(
            Map.entry("actionErrorChannel", actionErrorMatcher),
            Map.entry("productChannel", productErrorMatcher),
            Map.entry("eventErrorChannel", eventErrorMatcher)
        );
        service = new AlertService(config);
        service.slackClient = slackClient;
    }

    private AlertConfig.Matcher matcher(List<String> apps, List<String> errorCodes) {
        var matcher = new AlertConfig.Matcher();
        matcher.apps = apps;
        matcher.errorCodes = errorCodes;
        return matcher;
    }

    @Test
    void docURL() {
        assertThat(service.docURL("test", "test-id")).isEqualTo("http://kibana:5601/app/kibana#/doc/test-pattern/test-*?id=test-id&_g=()");
    }

    @Test
    void eligibleChannels() {
        var alert = new Alert();
        alert.app = "website";
        alert.severity = Severity.ERROR;
        alert.kibanaIndex = "trace";
        alert.errorCode = "java.lang.NullPointerException";
        assertThat(service.eligibleChannels(alert.app, alert.errorCode, alert.severity, alert.kibanaIndex)).contains("actionErrorChannel");

        alert = new Alert();
        alert.app = "website";
        alert.severity = Severity.ERROR;
        alert.kibanaIndex = "trace";
        alert.errorCode = "PRODUCT_ERROR";
        assertThat(service.eligibleChannels(alert.app, alert.errorCode, alert.severity, alert.kibanaIndex)).contains("actionErrorChannel", "productChannel");

        alert = new Alert();
        alert.app = "website";
        alert.severity = Severity.ERROR;
        alert.kibanaIndex = "event";
        alert.errorCode = "EVENT_ERROR";
        assertThat(service.eligibleChannels(alert.app, alert.errorCode, alert.severity, alert.kibanaIndex)).contains("eventErrorChannel");
    }

    @Test
    void alertKey() {
        Alert alert = alert(Severity.WARN, "NOT_FOUND");
        alert.action = "action";
        assertThat(service.alertKey(alert)).isEqualTo("website/action/WARN/NOT_FOUND");
    }

    @Test
    void process() {
        service.process(alert(Severity.WARN, "PATH_NOT_FOUND"));
        verifyNoInteractions(slackClient);
    }

    @Test
    void check() {
        assertThat(service.check(alert(Severity.ERROR, "CRITICAL_ERROR"), LocalDateTime.now()).notify).isTrue();
        assertThat(service.check(alert(Severity.WARN, "SLOW_SQL"), LocalDateTime.now()).notify).isTrue();

        Alert alert = alert(Severity.ERROR, "ERROR");
        LocalDateTime date = LocalDateTime.now();
        AlertService.Result result = service.check(alert, date);
        assertThat(result).matches(r -> r.notify && r.alertCountSinceLastSent == -1);

        result = service.check(alert, date.plusMinutes(30));
        assertThat(result).matches(r -> !r.notify);

        result = service.check(alert, date.plusHours(4));
        assertThat(result).matches(r -> r.notify && r.alertCountSinceLastSent == 1);
    }

    @Test
    void color() {
        LocalDateTime date = LocalDateTime.of(2020, 1, 1, 0, 0, 0);

        assertThat(service.color(Severity.WARN, date)).isEqualTo("#ff5c33");
        assertThat(service.color(Severity.WARN, date.plusWeeks(1))).isEqualTo("#ff9933");

        assertThat(service.color(Severity.ERROR, date)).isEqualTo("#a30101");
        assertThat(service.color(Severity.ERROR, date.plusWeeks(1))).isEqualTo("#e62a00");
    }

    @Test
    void message() {
        Alert alert = alert(Severity.WARN, "ERROR_CODE");
        alert.id = "id";
        alert.errorMessage = "message";
        alert.kibanaIndex = "action";

        assertThat(service.message(alert, 10)).isEqualTo("*[10]* WARN: *site / website*\n"
                + "_id: <http://kibana:5601/app/kibana#/doc/action-pattern/action-*?id=id&_g=()|id>\n"
                + "error_code: *ERROR_CODE*\n"
                + "message: message\n");

        alert.host = "host";
        assertThat(service.message(alert, 0)).isEqualTo("WARN: *site / website*\n"
                + "host: host\n"
                + "_id: <http://kibana:5601/app/kibana#/doc/action-pattern/action-*?id=id&_g=()|id>\n"
                + "error_code: *ERROR_CODE*\n"
                + "message: message\n");

        alert.action = "action";
        alert.host = null;

        assertThat(service.message(alert, 0)).isEqualTo("WARN: *site / website*\n"
                + "_id: <http://kibana:5601/app/kibana#/doc/action-pattern/action-*?id=id&_g=()|id>\n"
                + "action: action\n"
                + "error_code: *ERROR_CODE*\n"
                + "message: message\n");
    }

    private Alert alert(Severity severity, String errorCode) {
        var alert = new Alert();
        alert.app = "website";
        alert.severity = severity;
        alert.errorCode = errorCode;
        return alert;
    }
}
