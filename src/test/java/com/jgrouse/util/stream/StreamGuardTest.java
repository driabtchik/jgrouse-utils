package com.jgrouse.util.stream;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

public class StreamGuardTest {

    private boolean streamClosed;

    @Test
    void consume() {
        Stream<String> stream = Stream.of("foo", "bar").onClose(() -> streamClosed = true);
        assertThat(new StreamGuard<>(stream).<Long>consume(Stream::count)).isEqualTo(2);
        assertThat(streamClosed).isTrue();
    }

    @Test
    void construct_fromResourceAndStream_supplierWasCalled() throws Exception {
        AutoCloseable resource = mock(AutoCloseable.class);
        long res = new StreamGuard<>(() -> resource, supplier -> {
            supplier.get();
            return Stream.of("foo", "bar");
        }).consume(Stream::count);
        assertThat(res).isEqualTo(2);
        verify(resource).close();
    }

    @Test
    void construct_fromResourceAndStream_supplierWasNotCalled() throws Exception {
        AutoCloseable resource = mock(AutoCloseable.class);
        long res = new StreamGuard<>(() -> resource, supplier -> Stream.of("foo", "bar")).consume(Stream::count);
        assertThat(res).isEqualTo(2);
        verify(resource, times(0)).close();
    }

    @Test
    void construct_withExceptionInStreamCreation() throws Exception {
        AutoCloseable resource = mock(AutoCloseable.class);
        assertThatThrownBy(() -> new StreamGuard<>(() -> resource, supplier -> {
            supplier.get();
            throw new IllegalStateException("breaking processing");
        }).consume(Stream::count)).isInstanceOf(IllegalStateException.class);
        verify(resource).close();
    }

}