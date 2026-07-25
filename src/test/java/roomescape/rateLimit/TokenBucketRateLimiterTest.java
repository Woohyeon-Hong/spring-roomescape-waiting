package roomescape.rateLimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TokenBucketRateLimiterTest {

    @DisplayName("가용한 토큰 수만큼만 소비에 성공하고, 토큰이 없으면 tryConsume이 false를 반환한다.")
    @Test
    void tryConsumeTest_decrease_token_as_much_as_available() {
        //given
        AtomicLong frozenClock = new AtomicLong(0);
        long capacity = 3L;
        double refillPerSec = 1.0;

        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(capacity, refillPerSec, frozenClock::get);

        //when & then
        assertThat(limiter.tryConsume()).isTrue();
        assertThat(limiter.tryConsume()).isTrue();
        assertThat(limiter.tryConsume()).isTrue();
        assertThat(limiter.tryConsume()).isFalse();
    }

    @DisplayName("마지막 토큰 발급 이후 경과 시간 × refillPerSec 만큼 토큰이 충전된 뒤 소비된다.")
    @Test
    void tryConsumeTest_refill_before_decrease_token() {
        //given
        AtomicLong frozenClock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1.0, frozenClock::get);

        limiter.tryConsume();
        limiter.tryConsume();
        limiter.tryConsume();

        //when
        frozenClock.addAndGet(2 * 1000000000); // 2초 경과

        //then
        assertThat(limiter.tryConsume()).isTrue();
        assertThat(limiter.tryConsume()).isTrue();
        assertThat(limiter.tryConsume()).isFalse();
    }

    @DisplayName("오래 기다려도 토큰은 capacity를 넘게 쌓이지 않는다.")
    @Test
    void tryConsumeTest_not_over_capacity() {
        //given
        AtomicLong frozenClock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 1.0, frozenClock::get);

        frozenClock.addAndGet(100 * 1000000000); // 100초 경과

        assertThat(limiter.tryConsume()).isTrue();
        assertThat(limiter.tryConsume()).isTrue();

        //then
        assertThat(limiter.tryConsume()).isFalse();
    }

    @DisplayName("capacity에 따라 거부 시점이 달라진다.")
    @ParameterizedTest(name = "capacity={0} → {1}번째 요청에서 거부")
    @CsvSource({"5, 6", "1, 2", "3, 4"})
    void tryConsumeTest_reject_depend_on_capacity (long capacity, int rejectAt) {
        //given
        AtomicLong frozenClock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(capacity, 1.0, frozenClock::get);

        //when & then
        for (int i = 1; i < rejectAt; i++) {
            assertThat(limiter.tryConsume()).as("%d번째 요청", i).isTrue();
        }

        assertThat(limiter.tryConsume()).as("%d번째 요청은 거부", rejectAt).isFalse();
    }

    @DisplayName("동시 요청 여러 개 중 capacity개만 통과한다.")
    @Test
    void tryConsumeTest_concurrent() throws InterruptedException {
        //given
        long capacity = 3;
        int threadCount = 20;

        AtomicLong frozenClock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(capacity, 1.0, frozenClock::get);

        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger passed = new AtomicInteger();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

        //when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    if (limiter.tryConsume()) {
                        passed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        start.countDown();
        executorService.shutdown();

        //then
        assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        assertThat(passed.get()).isEqualTo(capacity);
    }

    @DisplayName("토큰이 한 개 이상 가용할 때 까지 남은 시간을 반환한다.")
    @Test
    void retryAfterSecondsTest() {
        //given
        AtomicLong frozenClock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 1.0, frozenClock::get);

        limiter.tryConsume();
        limiter.tryConsume();

        //when & then
        assertThat(limiter.retryAfterSeconds()).isEqualTo(1);
    }

    @DisplayName("필요한 대기 시간이 정수 초로 딱 떨어지지 않으면 올림해서 반환한다.")
    @Test
    void retryAfterSecondsTest_round_up_fractional_wait() {
        //given
        AtomicLong frozenClock = new AtomicLong(0);
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 0.4, frozenClock::get);

        limiter.tryConsume();

        //when & then
        // 남은 토큰 0, 1개 채우려면 (1 - 0) / 0.4 = 2.5초 → 올림해서 3초
        assertThat(limiter.retryAfterSeconds()).isEqualTo(3);
    }
}
