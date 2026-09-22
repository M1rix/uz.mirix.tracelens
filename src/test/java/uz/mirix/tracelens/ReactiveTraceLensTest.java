package uz.mirix.tracelens;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uz.mirix.tracelens.internal.TraceContext;
import uz.mirix.tracelens.internal.TraceContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveTraceLensTest {

    @Test
    void recordsCustomSpanFromReactorContext() {
        TraceContext context = new TraceContext("GET", "/prices", 10);

        StepVerifier.create(
                ReactiveTraceLens.trace("pricing-rules", Mono.just("ok"))
                    .contextWrite(view -> view.put(TraceContextHolder.REACTOR_KEY, context))
            )
            .expectNext("ok")
            .verifyComplete();

        TraceReport report = context.finish("/prices", 200, null, System.nanoTime());

        assertThat(report.spans())
            .filteredOn(span -> span.category() == TraceCategory.CUSTOM)
            .singleElement()
            .satisfies(span -> {
                assertThat(span.name()).isEqualTo("pricing-rules");
                assertThat(span.attributes()).containsEntry("signal", "ON_COMPLETE");
            });
    }

    @Test
    void isNoOpWithoutActiveReactiveTrace() {
        StepVerifier.create(ReactiveTraceLens.trace("pricing-rules", Mono.just(42)))
            .expectNext(42)
            .verifyComplete();
    }
}
